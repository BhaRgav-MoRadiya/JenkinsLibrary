package net.media
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder


/*
  Abort builds with custom message.
  This aborts the build not fail it.
*/
def abortBuild(String msg){
    //currentBuild variable is available by default.
    currentBuild.result = 'ABORTED'
    error(msg)
}

def setDefaults(Map constants){
  if(!constants.containsKey('dockerfilePath')){
    constants['dockerfilePath']="Dockerfile"
  }

}

def flockMessage(String url,String msg){
  sh """
  #!/bin/bash
  set +x
  curl -sX POST ${url} -H "Content-Type: application/json" -d '{
  "text": "${msg}"
  }'
  set -x
  """
}

def prerequisite(String path,String msgForFlock){

  stage("check for prerequisite"){
		if (fileExists("${path}")) {
    	echo 'Found Dockerfile..!!'
      msgForFlock+="Found Dockerfile..!!"
		} else {
      msgForFlock+="Dockerfile is missing at path specified..!!"
			abortBuild("Dockerfile is missing..!!")
		}
	}
}

def createRepoIfNotExists(Map constants){
  stage("Check for Repository"){
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

}


def buildImage(Map constants){

  	stage("build docker image"){
  					customImage=docker.build("${NAME}:${env.gitlabAfter}")
  	}

}

def pushImage(Map constants){

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
}


def manageTaskDefinition(Map constants){

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
}

def manageService(Map constants){

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

}

return this
