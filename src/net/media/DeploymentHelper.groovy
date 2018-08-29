package net.media


/*
  Zip the given files.
*/
def zipd(String inclusion, String fileName, String targetPath){
  //fileName is the jar to be deployed.
  //inclusion are the files provided by user to be zipped using includeInZip
  def zipper=  """
      cd ${targetPath}
      zip -r ${fileName}.zip ${fileName} ${inclusion}
  """
  def zipped = sh( script: zipper, returnStatus: true)
  if( zipped == 0)
    print("[DEPLOY LIB] zip success.")
  else
    abortBuild("[DEPLOY LIB] zip failed for ${filename} and ${inclusion}")

}


/*
  Initiate rsync of zip with destination IP addresses.
*/
def initMonolithDelivery(Map properties, String fileName, String targetPath){
  for(ip in properties['ip']){
    def rsync = "rsync -e 'ssh -o StrictHostKeyChecking=no' -avrzP ${targetPath}/${fileName} ${properties['user']}@${ip}:${properties['destinationPath']} > /dev/null"
		try {
    	def syncStatus = sh (script: rsync, returnStdout: true)
			print("Sync success for ${properies['ip']}")
		} catch(Exception e){
			print(syncStatus)
			abortBuild("rsync failed.")
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
  if( splitter.length() !=4 )
    abortBuild("Namespace enforcement failed. Expected format '{TeamName}/{Environment}/{ProjectName}/{ApplicationName}'")

  print("Enforcing namespace.")
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
