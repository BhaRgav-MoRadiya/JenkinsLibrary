

/*
			def constants = [:]
			constants['user'] = 'root'
			constants['ip'] = ['172.17.0.3']
			constants['zip'] = true //or false
			constants['includeInZip'] = "lib/"
			constants['destinationPath'] = '/root/hello'
			constants['dockerize'] = false
			constants['type'] = 'jar'
			constants['targetPath'] = "single-module/target"
			constants['appName']= "autoopts/crawler/proxytunnel"
			constants['tag'] = "prod"
			constants['marathonInstances'] = 2
			constants['marathonForce'] = true
			constants['dockerfile'] = "Dockerfile.en"
			constants['onlyBuild'] = true
			constants['marathonEndpoint'] = "http://dcos-master-1.og.reports.mn:8080/v2/apps/"
			constants['customArgs']  = "--build-arg deployment_env=prod"
*/


import net.media.DeploymentHelper

def call(Map constants){

	//def aws_docker_command = """docker run --rm -t \$(tty &>/dev/null && echo "-i") -e "AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}" -e "AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY}" -e "AWS_DEFAULT_REGION=${REGION}" mesosphere/aws-cli """
	def customImage = ''
	helper = new DeploymentHelper()
	//helper.first_test("funny")
	def taskDef= libraryResource 'net/media/shell/taskdef.json'
	writeFile file: "taskDefinition.json", text: taskDef
	sh "sed -e 's/IMAGE_TAG/abccdd/g' taskDefinition.json > task_def.json"

	sh "cat task_def.json"


  return true

}
