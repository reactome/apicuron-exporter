// This Jenkinsfile is used by Jenkins to run the 'APICURON Exporter' step of Reactome's release.
// It requires that the 'GraphImporter' step has been run successfully before it can be run.

import org.reactome.release.jenkins.utilities.Utilities

// Shared library maintained at 'release-jenkins-utils' repository.
def utils = new Utilities()

pipeline {
    agent any
    // Set output folder where files generated by step will be stored.
    environment {
        OUTPUT_FOLDER = "apicuron"
    }

    stages {
        // This stage checks that upstream project 'DiagramConverter' was run successfully.
        stage('Check GenerateGraphDatabaseAndAnalysisCore build succeeded') {
            steps {
                script {
                    utils.checkUpstreamBuildsSucceeded("GenerateGraphDatabaseAndAnalysisCore")
                }
            }
        }
        // This stage builds the jar file using maven.
        stage('Setup: Build jar file') {
            steps {
                script {
                    sh "mvn clean package -P Reactome-Server,production -DskipTests"
                }
            }
        }
        // Execute the jar file, producing interactions files for Human interactions and for all species interactions.
        stage('Main: Run Interaction-Exporter') {
            steps {
                script {
                    sh "mkdir -p ${env.OUTPUT_FOLDER}"
                    withCredentials([usernamePassword(credentialsId: 'neo4jUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]) {
                        sh "java -Xmx${env.JAVA_MEM_MAX}m -jar target/apicuron-exporter-exec.jar"
                    }
                }
            }
        }
    }
}
