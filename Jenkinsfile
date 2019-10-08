pipeline {
  agent any
  stages {
    stage('A') {
      steps {
        build 'install'
      }
    }
    stage('B') {
      steps {
        retry(count: 2) {
          echo 'Hey'
        }

      }
    }
  }
}