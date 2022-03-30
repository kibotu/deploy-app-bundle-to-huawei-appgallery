def functions

pipeline {

    agent any

    stages {

        stage('Prepare') {
            steps {
                script {
                    functions = load "deploy-bundle-to-huawei-appgallery/PipelineFunctions.groovy"
                }
            }
        }

        stage("Deploy Bundle to Huawei AppGallery") {

            steps {
                script {

                    archiveArtifacts artifacts: 'app/build/outputs/mapping/release/mapping.txt', fingerprint: true
                    archiveArtifacts artifacts: 'app/build/outputs/bundle/release/*.aab', fingerprint: true

                    def aab = "app/build/outputs/bundle/release/app-release.aab"
                    if (!fileExists()) error("File does not exist. '*.aab' at: $aab")

                    def mapping = "app/build/outputs/mapping/release/mapping.txt"
                    if (!fileExists(mapping)) error("File does not exist. 'mapping.txt' at: $mapping") // todo upload mapping

                    withCredentials([
                            string(credentialsId: 'APP_HUAWEI_SECRET_CLIENT_ID', variable: 'HUAWEI_SECRET_CLIENT_ID'),
                            string(credentialsId: 'APP_HUAWEI_CLIENT_SECRET', variable: 'HUAWEI_CLIENT_SECRET'),
                            string(credentialsId: 'APP_HUAWEI_SECRET_APP_ID', variable: 'HUAWEI_SECRET_APP_ID')
                    ]) {
                        functions.deployBundleToAppGallery(aab, clientId, clientSecret, appId)
                    }
                }
            }
        }
    }

    post {
        cleanup {
            cleanWs()
        }
    }
}
