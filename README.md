# Declarative Jenkins
![](https://i.imgur.com/du4qR2C.png)
> The hero you need, but not what you deserve.

## Functions
### deploy(Map properties)
Accepts a Hashmap with deployment properties.

| Param | Meaning | Type |Example value | Works for | Required |
|------| ------- |  -------| -------|    -------| -------|
|user| username with access to box| String |root | monolithic | yes
|ip| ip for the boxes | Array of strings | ['127.0.0.1', '127.0.0.2'] | monolithic| yes
| zip | To compress the build output | Boolean | true / false | Monolithic | no
|includeInZip | Files other than war or jar to be included | Space separated string| "lib/" or "lib/ other_dir/"| monolithic| no
|destinationPath| Path to deployment dir on server | String| /opt/project/ | monolithic | yes |
| dockerize | Whether to create docker image | Boolean | true / false | containers | yes |
