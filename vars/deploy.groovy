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
			params['parametrizedMesos'] = {
				"id": "autoopts3/crawler/proxytunnel",
				"instances": 2
			}
*/


import DeploymentHelper

def call(Map properties){
	def targetPath = ""
	def includeInZip = ""

	helper = new DeploymentHelper()

	if(properties.containsKey('dockerize') && properties['dockerize']){
		enforceNamespace(properties['appName']) 
	}

	if( properties.containsKey("targetPath"))
		targetPath = "${WORKSPACE}/" + properties['targetPath']
	else
		targetPath = "${WORKSPACE}/target"

	def getFilePath = "find ${targetPath}/ -maxdepth 1 -name '*.${properties['type']}' 2>/dev/null"


	try {
			filePath = sh (script: getFilePath, returnStdout: true).trim()
			fileName = filePath.split("/")[-1]
		} catch(Exception e){
			abortBuild("[DEPLOY LIB] PATH MOST LIKELY WRONG FOR TARGET DIRECTORY. SCRIPT LOOKING INTO  ${targetPath}. Use relative paths for targetPath param.")
		}


	if( properties['zip'] == true ){
			if( properties.containsKey("includeInZip"))
				includeInZip = properties['includeInZip']
			helper.zipd("${includeInZip}", fileName, targetPath)
	}


	if(properties['dockerize'] == true){	
		docker = """
			cd ${targetPath}
			docker build -t r.reports.mn/${properties['appName']}:${tag} -f Dockerfile ${WORKSPACE}
			docker push r.reports.mn/${properties['appName']}:${tag}
		"""
		buildStatus = sh(script: docker, returnStatus: true)
		if(buildStatus==0)
			print("Build push complete.")
		else
			print("Build failure.")
  }

	else {
		helper.initMonolithDelivery(properties, fileName ,targetPath)
	}

}

