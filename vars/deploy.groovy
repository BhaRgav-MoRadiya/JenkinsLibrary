

/*
			def constants = [:]
			constants['region'] = 'us-east-1'
			constants['credsIdForECR'] = ['172.17.0.3']
			constants['credsIdForECS'] = true //or false
			constants['ecrName'] = "lib/"
			constants['awsAccountUri'] = '/root/hello'
			constants['ecsFamily'] = false
			constants['ecsServiceName'] = 'jar'
			constants['ecsClusterName'] = "single-module/target"
			constants['ecsDesiredCount']= "autoopts/crawler/proxytunnel"
			constants['productionBranch'] = "prod"
			constants['stagingBranch'] = 2
*/


import net.media.DeploymentHelper

def call(Map constants){

	//def aws_docker_command = """docker run --rm -t \$(tty &>/dev/null && echo "-i") -e "AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}" -e "AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY}" -e "AWS_DEFAULT_REGION=${REGION}" mesosphere/aws-cli """
	def customImage = ''
	helper = new DeploymentHelper()
	helper.first_test(constants)
	echo constants["abcd"]
	def taskDef= libraryResource 'net/media/shell/taskdef.json'
	writeFile file: "taskDefinition.json", text: taskDef
	sh "sed -e 's/IMAGE_NAME/abccdd/g' taskDefinition.json > task_def.json"

	sh "cat task_def.json"


  return true

}
