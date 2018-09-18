package net.media
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

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
			sh("unzip -o ${fileName}")
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
	def baseUrl = "http://dcos-master-1.og.reports.mn:8080/v2/apps/"
	def marathonEndpoint = ""
	def appName = "/" + properties['appName']
	def resourceUrl = baseUrl + appName

	if(properties.containsKey('marathonInstances')){
		def runnerCount = properties['marathonInstances']
		def json = new JsonBuilder()
		def root = json id: appName, instances: runnerCount
		def payload = json.toString()
		marathonEndpoint = "curl -H 'Content-type: application/json' -s -o /dev/null -w '%{http_code}' -X PUT -d '${payload}' '${resourceUrl}'"
	}
	else{
		resourceUrl += "/restart"
		if(properties.containsKey('marathonForce') && properties['marathonForce']==true)
			resourceUrl += "?force=true"
		marathonEndpoint = "curl -XPOST -s -o /dev/null -w '%{http_code}' ${resourceUrl}"
	}
		


	def httpStatus = sh (script: marathonEndpoint, returnStdout: true)
	if(httpStatus != "200")
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

/*
	Docker tag controllers
===========================================================================================
*/

/*
	Remove docker images older than 3 builds
*/
def dockerRMI(def image, def tag){
  dir("script"){
      def shellScript = libraryResource 'net/media/shell/deleteImages.sh'
      writeFile file: "deleteImages.sh", text: shellScript
      sh 'cdr=$(pwd);chmod +x $cdr/deleteImages.sh'
			def currentDirectory = pwd()
      def registry = sh(script:" ${currentDirectory}/deleteImages.sh $image $tag", returnStatus:true)
      if(registry==0)
        print("Old image deletion successful.")
      else
        print("Old image deletion failed.")
  }
}

/*
	Get all tags associated with an image
*/
def getAllTags(String appName){
	def jsonSlurper = new JsonSlurper()
	def response = new URL("http://r.reports.mn/v2/${appName}/tags/list").text
	def object = jsonSlurper.parseText(response)
	if(!object.containsKey("tags"))
		abortBuild("No image found for ${appName} in registry.")

	return object.tags
}

/*
	Get auto-incrementing tag for the new
	docker image.
*/
def getIncrementingTag(String appName){
	def tags = getAllTags(appName)
	tags.removeAll(["latest", "prod"])

	//in case there is no tag
	if(tags.size() < 1)
		return 1

	tags = tags.sort()

	def lastTag = tags[-1]
	lastTag = lastTag.toInteger()
	return lastTag + 1
}

def manageTag(def properties){
	def nextTag = getIncrementingTag(properties['appName'])
	print("nexttag : ${nextTag}")
	def removeTag = nextTag-3
	if(!(removeTag<1))
		dockerRMI(properties['appName'], removeTag)
	return nextTag
}

/*
end tag methods
===========================================================================================
*/



return this
