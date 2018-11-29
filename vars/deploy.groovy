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
	def targetPath = ""
	def includeInZip = ""

	helper = new DeploymentHelper()

	if(properties.containsKey('dockerize') && properties['dockerize']){
		helper.enforceNamespace(properties['appName']) 
	}

	if(properties['type'] == 'war' || properties['type'] == 'jar'){

		targetPath = (properties.containsKey("targetPath") ?  "${WORKSPACE}/" + properties['targetPath'] : "${WORKSPACE}/target") 
		
		def getFilePath = "find ${targetPath}/ -maxdepth 1 -name '*.${properties['type']}' 2>/dev/null"

		try {
				filePath = sh (script: getFilePath, returnStdout: true).trim()
				fileName = filePath.split("/")[-1]
			} catch(Exception e){
				abortBuild("[DEPLOY LIB] PATH MOST LIKELY WRONG FOR TARGET DIRECTORY. SCRIPT LOOKING INTO  ${targetPath}. Use relative paths for targetPath param.")
			}

	}


	if( properties['zip'] == true ){
		stage('Zipping files'){
			if( properties.containsKey("includeInZip"))
				includeInZip = properties['includeInZip']
			helper.zipd("${includeInZip}", fileName, targetPath)
		}
	}


	if(properties['dockerize'] == true){
		stage('Building docker image'){
			extraParams = ""
			extraBoolean = false
			if(properties.containsKey("customArgs")){
				extraParams = properties['customArgs']
				extraBoolean = true
			}
			if(properties.containsKey("dockerfile")){
				extraParams += " -f ${properties['dockerfile']} ."
				extraBoolean = true
			}
			if(extraBoolean)
				app = docker.build("${properties['appName']}", extraParams)
			else
				app = docker.build("${properties['appName']}")
		}

		stage('Pushing to reports.mn'){
			docker.withRegistry('http://r.reports.mn')	{
				app.push('latest')
				def incTag = helper.manageTag(properties)
				incTag = incTag.toString()
				app.push(incTag)
			}
		}

		if(properties.containsKey('onlyBuild') && !properties['onlyBuild']){
			stage('Deploying to marathon'){
				helper.marathonRunner(properties)
			}
		}
  }
	else {
		stage("Delivering package"){
			helper.initMonolithDelivery(properties, fileName ,targetPath)
		}
	}

  return true

}

