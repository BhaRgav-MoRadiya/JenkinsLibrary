/*
	Share library to deploy jar or war files.
	Supports zipping, and dockerizing.
*/

/*
			def params = [:]
			params['user'] = 'root'
			params['ip'] = ['172.17.0.3']
			params['zip'] = true //or false
			params['includeInZip'] = "lib/"
			params['destinationPath'] = '/root/hello'
			params['dockerize'] = false
			params['type'] = 'jar'
			params['targetPath'] = "single-module/target"
			params['appName']= "autoopts/crawler/proxytunnel"
			params['tag'] = "prod"
			params['marathonInstances'] = 2
			params['marathonForce'] = true
			params['dockerfile'] = "Dockerfile.en"
			params['onlyBuild'] = true
			params['marathonEndpoint'] = "http://dcos-master-1.og.reports.mn:8080/v2/apps/"
			params['customArgs']  = "--build-arg deployment_env=prod"
*/


import net.media.DeploymentHelper

def call(Map properties){

	def aws_docker_command = """docker run --rm -t \$(tty &>/dev/null && echo "-i") -e "AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}" -e "AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY}" -e "AWS_DEFAULT_REGION=${REGION}" mesosphere/aws-cli """
	def customImage = ''
	helper = new DeploymentHelper()
	helper.first_test("funny")
	stage("check for prerequisite"){
		if ("${env.gitlabTargetBranch}" == "release" && fileExists('task_def.json') && fileExists('Dockerfile') && fileExists('variables.groovy')) {
    	echo 'prerequisite check passed..!!'
		} else {
			echo 'Either code is pushed to different branch than the one specified or required files are missing..!! required files:[task_def.json,Dockerfile,variables.groovy]'
    	currentBuild.result = 'ABORTED'
			return
		}
	}
	stage("fails if Repository exists"){
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
	}

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
		stage("update service"){
			try{
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
			}catch(ex){echo "error updating service..!!!"}
 		}

  return true

}
