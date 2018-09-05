# Declarative Jenkins
![](https://i.imgur.com/du4qR2C.png)
> The hero you need, but not what you deserve.

## Functions
### deploy(Map properties)
Accepts a Hashmap with deployment properties.

| Param | Meaning | Type |Example value | Works for | Required | Comment |
|------| ------- |  -------| -------|    -------| -------|  -------|
|user| username with access to box| String | "root" | Monolithic | Yes| |
|ip| ip for the boxes | Array of strings | ['127.0.0.1', '127.0.0.2'] | Monolithic| Yes| |
| zip | To compress the build output | Boolean | true / false | Monolithic | No| |
|includeInZip | Files other than war or jar to be included | Space separated string| "lib/" or "lib/ other_dir/"| Monolithic| No | Works only with zip=true|
|destinationPath| Path to deployment dir on server | String| /opt/project/ | Monolithic | Yes | |
|type| Type of deployment, i.e, jar or war | String | "jar" or "war"| Monolithic | Yes | Auto-picks war or jar generated on basis of this. |
|targetPath | custom /target for jenkins** | String | "first-module/target" | Monolithic | No |
| dockerize | Whether to create docker image | Boolean | true / false | Containers | Yes | |
|appName | Name used to create and deploy images or monolith apps. |String | "autoopts3/prod/crawler/proxytunnel", "teamName/env/projectName/applicationName" | Both | Yes| Library enforces a 4 layer naming convention as in example. Build will fail if not complied with. |
| tag | Tag for docker image |  String | **only values allowed:** "prod", "latest" | Containers | No |   |
|marathonInstances | set number of instances for marathon | Integer | 2 , 3 | Containers | no | If not given, a simple restart API call is made | |
| forceMarathon | Adds ?force=true to API call | Boolean | true / false | Containers | no |  |


** Required only in case your build path is other than workspace/target.
Example: Your build path is workspace/customApp/target 

#### Sample Jenkinsfile

```java
//Import shared library. "_" is essential

@Library('deploy')_
def properties = [:]
properties['user'] = "root"
properties['ip'] = ['172.17.0.3', '172.17.0.4']
properties['destinationPath'] = "/root/deploy/dir"
properties['type'] = "jar"
node {
    stage('Deployment'){
        deploy(properties)
    }
}
```