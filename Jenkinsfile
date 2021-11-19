@Library('shared-library@no-tests-more-properties') _

def ETHEREUM_PROPERTIES = [BLOCKCHAIN:"ethereum"]

pipeline {
  parameters {
    booleanParam(name: 'RUN_TEST', defaultValue: true, description: 'Run tests during build.')
  }
  agent none

  options {
    disableConcurrentBuilds()
  }
  environment {
    PREFIX = "protocol"
    CREDENTIALS_ID = "nexus-ci"
    ETHEREUM_STACK = "protocol-ethereum"
  }

  stages {
    stage('test') {
      agent any
      when {
        expression {
            return RUN_TEST.toBoolean()
        }
      }
      steps {
         sh 'mvn clean test -U'
      }
      post {
        always {
          script {
            def testsSummary = junit(testResults: '**/surefire-reports/*.xml', allowEmptyResults: true)
            step([ $class: 'JacocoPublisher', execPattern: '**/target/jacoco-aggregate.exec' ])

            def color = testsSummary.failCount > 0 ? "danger" : "good"
            slackSend(
              channel: "#protocol-duty",
              color: color,
              message: "\n *[ethereum-indexer] [${env.GIT_BRANCH}] Test Summary*: Total ${testsSummary.totalCount}, Failures: ${testsSummary.failCount}, Skipped: ${testsSummary.skipCount}, Passed: ${testsSummary.passCount}"
            )
          }
        }
      }
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
    stage("deploy to dev") {
      agent any
      when {
        allOf {
          anyOf { branch 'develop'; branch 'master' }
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
        deployStack(env.APPLICATION_ENVIRONMENT, env.ETHEREUM_STACK, env.PREFIX, env.IMAGE_TAG, [], ETHEREUM_PROPERTIES)
      }
    }
    stage("deploy to e2e") {
      agent any
      when {
        allOf {
          anyOf { branch 'develop'; branch 'master' }
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
        deployStack(env.APPLICATION_ENVIRONMENT, env.ETHEREUM_STACK, env.PREFIX, env.IMAGE_TAG, [], ETHEREUM_PROPERTIES)
      }
    }
    stage("deploy to staging") {
      agent any
      when {
        allOf {
          anyOf { branch 'develop'; branch 'master' }
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
        deployStack(env.APPLICATION_ENVIRONMENT, env.ETHEREUM_STACK, env.PREFIX, env.IMAGE_TAG, [], ETHEREUM_PROPERTIES)
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
