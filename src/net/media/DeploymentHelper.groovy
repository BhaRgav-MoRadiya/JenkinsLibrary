package net.media


/*
  Zip the given files.
*/
def zipd(String inclusion, String fileName, String targetPath){
  //fileName is the jar to be deployed.
  //inclusion are the files provided by user to be zipped using includeInZip
  def zipper=  "zip -r ${fileName}.zip ${fileName} ${inclusion}"
	dir(targetPath){
 		def zipped = sh( script: zipper, returnStatus: true)
  	if( zipped != 0)
    	abortBuild("[DEPLOY LIB] zip failed for ${filename} and ${inclusion}")

    print("[DEPLOY LIB] zip success.")
	}
 

}


/*
  Initiate rsync of zip with destination IP addresses.
*/
def initMonolithDelivery(Map properties, String fileName, String targetPath){

	String syncStatus=""
	def successIP = [] //keep track of successful deployments

  for(ip in properties['ip']){

 	 def rsync = "rsync -e 'ssh -o StrictHostKeyChecking=no' -avrzP ${targetPath}/${fileName} ${properties['user']}@${ip}:${properties['destinationPath']} 1>/dev/null"

		try {
    	syncStatus = sh (script: rsync, returnStdout: true)
			successIP.add(ip)
			print("Sync success for ${ip}")
		} catch(Exception e){
			print("Sync failed for ${ip}")
			print(syncStatus)
			if(successIP.size()>0){
				print("Initiating rollback")
				rollbackMonolith(properties, successIP, fileName)
			}
			abortBuild("Syncing to server(s) failed.")
		}
  }
}

/*
	Rollback deployments in case of failure in between deployment.
*/
def rollbackMonolith(Map properties, def successIP, String fileName){
	for(ip in successIP){
		def rollback = "ssh ${properties['user']}@${ip} 'rm ${properties['destinationPath']}/${fileName}'"
		try{
			def rolling = sh(script: rollback, returnStdout: true)
			print("Rollback complete for ${ip}")
		}catch(Exception e){
			print("Rollback failed.")
		}
	}
}



/*
  Abort builds with custom message.
  This aborts the build not fail it.
*/
def abortBuild(String msg){
    //currentBuild variable is available by default.
    currentBuild.result = 'ABORTED'
    error(msg)
}


/*
  Enforces namespaces for dockerized builds.
*/
def enforceNamespace(String appName){
  def splitter = appName.split("/")
  if( splitter.size() !=4 )
    abortBuild("Namespace enforcement failed. Expected format '{TeamName}/{Environment}/{ProjectName}/{ApplicationName}'")

  print("Enforcing namespace.")
}


/*
	Marathon container handling
*/
def marathonRunner(def properties){
	def baseUrl = "http://marathon.og.reports.mn/v2/apps/"
	def payload = ""
	def marathonEndpoint = "
	def appName = properties['appName']
	def resourceUrl = baseUrl + appName

	if(properties.containsKey('marathonInstances')){
		def runnerCount = properties['marathonInstances']
		payload  = '{"id": "${appName}", "instances": ${runnerCount}}'
		marathonEndpoint = "curl -H 'Content-type: application/json' -X PATCH -d ${payload}"
	}
	else{
		resourceUrl += "/restart"
		if(properties.containsKey('marathonForce') && properties['marathonForce']==true)
			resourceUrl += "?force=true"
		marathonEndpoint = "curl -XPOST ${resourceUrl}"
	}
		


	def status = sh (script: marathonEndpoint, returnStatus: true).trim()
	if(status != 0)
		abortBuild("Marathon deployment failed.")

	print("Marathon restart initiated.")
}


/*
  Check if all required params are provided
  for normal functioning of script.
*/
def propertiesVerifier(Map properties, Boolean dockerize){

  def required = ["user", "ip", "destinationPath", "type"] //required fields for program to work.
  def dockerizeRequired = ["appName", "tag"] //fields required for dockerizing

  if(dockerize==true){
    for( String param in dockerizeRequired ){
      if(!properties.containsKey(param))
        abortBuild("Missing required key: ${param}")
    }
  }

  for( String param in required ){
      if(!properties.containsKey(param))
        abortBuild("Missing required key: ${param}")
  }

}

return this
