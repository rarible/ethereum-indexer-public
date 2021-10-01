@Library('shared-library@no-tests-more-properties') _

def ETHEREUM_PROPERTIES = [BLOCKCHAIN:"ethereum"]
// def POLYGON_PROPERTIES = [BLOCKCHAIN:"polygon"]

pipeline {
  agent none

  options {
    disableConcurrentBuilds()
  }
  environment {
    PREFIX = "protocol"
    CREDENTIALS_ID = "nexus-ci"
    ETHEREUM_STACK = "protocol-ethereum"
//     POLYGON_STACK = "protocol-polygon"
  }

  stages {
    stage('test') {
      agent any
      steps {
         sh 'echo OK'
      }
//       post {
//         always {
//           junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
//           step([ $class: 'JacocoPublisher', execPattern: '**/target/jacoco-aggregate.exec' ])
//         }
//       }
    }
    stage('package and publish') {
      agent any
      when {
        anyOf { branch 'master'; branch 'develop'; branch 'release/*'; branch 'RPN-803-multi-chain-pipeline' }
        beforeInput true
      }
      steps {
        script {
          env.BRANCH_NAME = "${env.GIT_BRANCH}"
          env.IMAGE_TAG = "${env.BRANCH_NAME.replace('release/', '')}-${env.BUILD_NUMBER}"
          env.VERSION = "${env.IMAGE_TAG}"
        }
        deployToMaven(env.CREDENTIALS_ID)
        publishDockerImages(env.PREFIX, env.CREDENTIALS_ID, env.IMAGE_TAG)
      }
    }
    stage("deploy to prod") {
      agent any
      when {
        allOf {
          anyOf { branch 'release/*' }
          expression {
            input message: "Deploy to prod?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'prod'
      }
      steps {
        deployStack(env.APPLICATION_ENVIRONMENT, env.ETHEREUM_STACK, env.PREFIX, env.IMAGE_TAG, [], ETHEREUM_PROPERTIES)
//         deployStack(env.APPLICATION_ENVIRONMENT, env.POLYGON_STACK, env.PREFIX, env.IMAGE_TAG, [], POLYGON_PROPERTIES)
      }
    }
  }
  post {
    always {
      node("") {
        cleanWs()
      }
    }
  }
}
