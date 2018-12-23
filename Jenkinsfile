node('ci_aws'){
        checkout([
            $class: 'GitSCM',
            branches: [[name: 'master']],
            doGenerateSubmoduleConfigurations: false,
            userRemoteConfigs: [[url: 'git@tree.mn:sem/collegearena.com.git']]
            ])
		def customImage = ''
		// if("${env.gitlabTargetBranch}" != "release"){
		//     currentBuild.result = 'SUCCESS'
	    //     return
		// }
		def NAME=sh(returnStdout: true, script: "grep \"RepositoryName\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def REGION=sh(returnStdout: true, script: "grep \"Region\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def REPOSITORY_NAME=sh(returnStdout: true, script: "grep \"RepositoryURL\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def CLUSTER=sh(returnStdout: true, script: "grep \"Cluster\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def FAMILY=sh(returnStdout: true, script: "grep \"Family\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def SERVICE_NAME=sh(returnStdout: true, script: "grep \"ServiceName\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def DESIRED_COUNT=sh(returnStdout: true, script: "grep \"DesiredCount\" variables.txt |awk -F ':' '{print \$2}'").trim()
		def EXPOSED_PORT=sh(returnStdout: true, script: "grep \"ExposedPort\" variables.txt |awk -F ':' '{print \$2}'").trim()
    def aws_docker_command = """docker run --rm -t \$(tty &>/dev/null && echo "-i") -e "AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}" -e "AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY}" -e "AWS_DEFAULT_REGION=${REGION}" mesosphere/aws-cli """
		try{
		    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId:"sem-nv-sentimeta-deployment-key"]]) {
            			sh """
            			set +x
            			${aws_docker_command} ecr create-repository --repository-name ${NAME}
            			set -x
            			"""
                echo "New repository created...!!!"
            }
		}catch(ex){echo "repository already exists..!!!"}

		stage("build docker image"){

            customImage=docker.build("${NAME}:${env.gitlabAfter}")

		}
		stage("image push"){
			docker.withRegistry('https://778201844681.dkr.ecr.us-east-1.amazonaws.com','ecr:us-east-1:sem-nv-sentimeta-deployment-key') {
		        sh "mkdir -p .docker; cat /root/.dockercfg > $WORKSPACE/.docker/config.json"
        		withEnv(['DOCKER_CONFIG=$WORKSPACE/.docker']) {
            		withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId:"sem-nv-sentimeta-deployment-key"]]) {
            			res = sh(returnStdout: true, script:"${aws_docker_command} ecr get-login --no-include-email --region ${REGION}").trim()
            			sh """
            			set +x
            			eval ${res}
            			set -x
            			"""
            		}
            customImage.push()
        		}
    		}

		}
	  stage("register/update task definition"){

		withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId:"sem-nv-sentimeta-ecs-key"]]) {
	sh "rm -f task_def.json"
		sh "sed -e 's/IMAGE_TAG/${NAME}:${env.gitlabAfter}/g' -e 's/FAMILY_NAME/${FAMILY}/g' -e 's/CONTAINER_NAME/${FAMILY}/g' -e 's/EXPOSED_PORT/${EXPOSED_PORT}/g' taskdef.json > task_def.json"
		def JSONFILE = readFile('task_def.json')
		sh """
		#!/bin/bash
		set +x
		${aws_docker_command} ecs register-task-definition --family ${FAMILY} --cli-input-json '${JSONFILE}'  --region ${REGION}
		set -x
		"""
		}
        }
       try{
        stage("update service"){
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId:"sem-nv-sentimeta-ecs-key"]]) {
		sh"""
		#!/bin/bash
		SERVICES=`${aws_docker_command} ecs describe-services --services ${SERVICE_NAME} --cluster ${CLUSTER} --region ${REGION} | jq .failures[]`
		#Get latest revision
		REVISION=`${aws_docker_command} ecs describe-task-definition --task-definition ${FAMILY} --region ${REGION} | jq .taskDefinition.revision`

		if [ "\$SERVICES" == "" ]; then
		  echo "Updateing existing service..!!"
		  ${aws_docker_command} ecs update-service --cluster ${CLUSTER} --region ${REGION} --service ${SERVICE_NAME} --task-definition ${FAMILY}:\${REVISION} --desired-count ${DESIRED_COUNT}
		else
		  echo "Service doesn't exist...!!"
		fi

		 """
		}

	 }
    }catch(ex){echo "error updating service..!!!"}
}
