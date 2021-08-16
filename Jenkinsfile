@Library('shared-library@no-tests') _

pipeline {
  agent none

  options {
    disableConcurrentBuilds()
  }

  stages {
//     stage('test') {
//       agent any
//       steps {
//         sh 'mvn clean test'
//       }
//       post {
//         always {
//           junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
//           step([ $class: 'JacocoPublisher', execPattern: '**/target/jacoco-aggregate.exec' ])
//         }
//       }
//     }
    stage('package and publish') {
      agent any
      when {
        anyOf { branch 'master'; branch 'develop'; branch 'release/*'; branch 'RPN-803-multi-chain-pipeline' }
        beforeInput true
      }
      steps {
        sh 'mvn clean package -DskipTests'

        script {
          env.BRANCH_NAME = "${env.GIT_BRANCH}"
          env.IMAGE_TAG = "${env.BRANCH_NAME.replace('release/', '')}-${env.BUILD_NUMBER}"
          env.VERSION = "${env.IMAGE_TAG}"
        }
        publishDockerImages(prefix, credentialsId, env.IMAGE_TAG)
      }
    }
    stage("deploy to dev") {
      agent any
      when {
        allOf {
          anyOf { branch 'develop'; branch 'master'; branch 'RPN-803-multi-chain-pipeline' }
          expression {
            input message: "Deploy to dev?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'dev'
      }
      steps {
        withEnv(["BLOCKCHAIN=ethereum"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
        withEnv(["BLOCKCHAIN=polygon"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
      }
    }
    stage("deploy to e2e") {
      agent any
      when {
        allOf {
          anyOf { branch 'master'; branch 'develop'; branch 'release/*' }
          expression {
            input message: "Deploy to e2e?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'e2e'
      }
      steps {
        withEnv(["BLOCKCHAIN=ethereum"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
      }
    }
    stage("deploy to staging") {
      agent any
      when {
        allOf {
          anyOf { branch 'release/*' }
          expression {
            input message: "Deploy to staging?"
            return true
          }
        }
        beforeAgent true
      }
      environment {
        APPLICATION_ENVIRONMENT = 'staging'
      }
      steps {
        withEnv(["BLOCKCHAIN=ethereum"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
        withEnv(["BLOCKCHAIN=polygon"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
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
        withEnv(["BLOCKCHAIN=ethereum"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
        withEnv(["BLOCKCHAIN=polygon"]) {
            deployStack("${APPLICATION_ENVIRONMENT}", "protocol-${BLOCKCHAIN}", "protocol", env.IMAGE_TAG)
        }
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
