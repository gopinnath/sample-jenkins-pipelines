node {
    stage('Preparation') { 
		def sourceExists = fileExists 'property-source'
		if (!sourceExists){
			new File('property-source').mkdir()
		}
		dir ('property-source') {
			git changelog: false, credentialsId: 'd7d5bf7d-b4ca-490f-8752-1ecf9c930dd9', poll: false, url: 'https://github.com/gopinnath/property-source.git'
		}
        
		def targetExists = fileExists 'property-target'
		if (!targetExists){
			new File('property-target').mkdir()
		}
		dir ('property-target') {
			git changelog: false, credentialsId: 'd7d5bf7d-b4ca-490f-8752-1ecf9c930dd9', poll: false, url: 'https://github.com/gopinnath/property-target.git'
		}
    }
    stage('Encrypt') {
		
        sh '''
			WORKINGFOLDER=working
			if [ -d "$WORKINGFOLDER" ]; then
				rm -R $WORKINGFOLDER
				echo "$WORKINGFOLDER deleted."
			fi

			mkdir $WORKINGFOLDER
			cp property-source/dev/* $WORKINGFOLDER/
			chmod +777 $WORKINGFOLDER/*.config
		'''
		
		ansibleVault action: 'encrypt', content: '', input: 'working/application.config', installation: 'Ansible', output: '', vaultCredentialsId: 'encryptstring'
    }
	
	stage('Move and Commit')	{
		def branchName="${env.JOB_NAME}-${env.BUILD_NUMBER}"
		sh '''
			cp -rf working/*.config property-target/dev/
			cd property-target
			git checkout -b build/'''+branchName+'''
			git add .
			git config user.name "Gopinath Radhakrishnan"
			git config user.email "gopinnath@gmail.com"
			git commit -m "Jenkins Encrypt Commit"
		'''
		sshagent(['JenkinsSSH']) {
			sh """
				cd property-target
				git remote rm origin
				git remote add origin git@github.com:gopinnath/property-target.git
				git push origin build/${env.JOB_NAME}-${env.BUILD_NUMBER}
			"""
		}
	}
	
    stage('Post Build Cleanup') {
		sh 'Nothing to Cleanup for now.'
    }
}
