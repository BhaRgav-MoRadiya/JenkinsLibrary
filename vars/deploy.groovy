

/*
			def constants = [:]
			constants['dockerfilePath'] = '_relative_path_from_root_directory_to_Dockerfile_if_not_placed_in_root_directory_'
			constants['awsAccountUri'] = '_same_for_whole_media.net_'
			constants['region'] = '_aws_region_where_repository_exists_or_to_be_created_'
			constants['credsIdForECR'] = '_value_stored_in_jenkins_credsStore_'
			constants['credsIdForECS'] = '_value_stored_in_jenkins_credsStore_'
			constants['ecrProdName'] = "_name_of_the_repository_in_ecr_for_production_"
			constants['ecrStageName'] = "_name_of_the_repository_in_ecr_for_staging_"
			constants['ecsProdFamily'] = '_family_name_for_task_definition_in_ecs_'
			constants['ecsStageFamily'] = '_family_name_for_task_definition_in_ecs_'
			constants['ecsProdServiceName'] = '_service_name_for_ecs'
			constants['ecsStageServiceName'] = '_service_name_for_ecs'
			constants['ecsProdClusterName'] = "_cluster_name_in_ecs_"
			constants['ecsStageClusterName'] = "_cluster_name_in_ecs_"
			constants['ecsProdDesiredCount']= "_desired_count_service_to_manage_tasks"
			constants['ecsStageDesiredCount']= "_desired_count_service_to_manage_tasks"
			constants['productionBranch'] = "_branch_to_which_if_code_is_pushed_will_deploy_service_in_prod_cluster_"
			constants['stagingBranch'] = "_branch_to_which_if_code_is_pushed_will_deploy_service_in_staging_cluster_"
			constants['flockWebhook'] = "_send_notification_to_this_flock_group_"

*/


import net.media.DeploymentHelper

def call(Map constants){

	//def aws_docker_command = """docker run --rm -t \$(tty &>/dev/null && echo "-i") -e "AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}" -e "AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY}" -e "AWS_DEFAULT_REGION=${REGION}" mesosphere/aws-cli """
	def customImage = ''
	def msgForFlock=''
	helper = new DeploymentHelper()
	helper.setDefaults(constants)
	helper.prerequisite(constants["dockerfilePath"],msgForFlock)
	constants['flockWebhook']="https://api.flock.com/hooks/sendMessage/742f4f19-559a-417e-872c-0e51692a0a75"
	helper.flockMessage(constants["flockWebhook"],msgForFlock)
	//helper.flockMessage(constants["flockWebhook"],"testing....!!!!")
	// helper.setDefaults(constants)
	// echo constants["abcd"]
	// def taskDef= libraryResource 'net/media/shell/taskdef.json'
	// writeFile file: "taskDefinition.json", text: taskDef
	// sh "sed -e 's/IMAGE_NAME/abccdd/g' taskDefinition.json > task_def.json"
	//
	// sh "cat task_def.json"


  return true

}
