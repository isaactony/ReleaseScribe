pipeline {
  agent any
  environment {
    ANTHROPIC_API_KEY = credentials('ANTHROPIC_API_KEY')
    GITHUB_TOKEN      = credentials('GITHUB_TOKEN') // repo:read
  }
  stages {
    stage('Checkout') { 
      steps { 
        checkout scm
        sh 'git fetch --tags --prune' 
      } 
    }
    stage('Build') { 
      steps { 
        sh 'mvn -q -DskipTests package' 
      } 
    }
    stage('Generate Release Notes') {
      steps {
        script {
          // Choose your range: by tags (recommended)
          def sinceTag = sh(
            script: 'git describe --tags --abbrev=0 --tags "$(git describe --tags --abbrev=0)^"',
            returnStdout: true
          ).trim()
          
          def untilTag = sh(
            script: 'git describe --tags --abbrev=0',
            returnStdout: true
          ).trim()
          
          echo "Generating release notes for range: ${sinceTag}..${untilTag}"
          
          sh """
            java -jar target/relnotes.jar \\
              --provider github \\
              --owner ${env.GIT_URL.split('/')[3].replace('.git', '')} \\
              --repo ${env.GIT_URL.split('/')[4].replace('.git', '')} \\
              --since-tag "${sinceTag}" \\
              --until-tag "${untilTag}" \\
              --out-dir dist \\
              --publish-github-release true \\
              --verbose
          """
        }
      }
    }
    stage('Archive') {
      steps { 
        archiveArtifacts artifacts: 'dist/*.md', fingerprint: true 
      }
    }
  }
  post {
    always {
      cleanWs()
    }
    success {
      echo 'Release notes generated successfully!'
    }
    failure {
      echo 'Failed to generate release notes'
    }
  }
}
