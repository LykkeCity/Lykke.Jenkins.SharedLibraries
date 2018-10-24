def call(Map pipelineParams) {
    pipeline {
        agent {label 'agent1'}
        stages {
            stage('checkout git') {
                steps {
                    git branch: pipelineParams.branch, url: pipelineParams.scmUrl
                }
            }
            stage("build") {
                steps {
                    sh "dotnet build --configuration Release"
                }
            }
            stage("test") {
                steps {
                    sh "dotnet test tests/**/*.csproj --configuration Release --no-build"
                }
            }
            stage("publish") {
                steps {
                  sh "dotnet publish src/${pipelineParams.publishProject}/${pipelineParams.publishProject}.csproj --configuration Release --no-restore --output ${env.WORKSPACE}/app/${pipelineParams.publishProject}"
                }
            }
            stage("build docker image") {
                steps{
                    script {
                        dockerImage = docker.build("${pipelineParams.dockerImageName}:${pipelineParams.dockerImageTag}", "app/${pipelineParams.publishProject}")
                    }
                }
            }
            stage("deploy docker image") {
                steps{
                    script {
                      docker.withRegistry( "", pipelineParams.dockerCredentialsId ) {
                          dockerImage.push()
                      }
                    }
                }
            }
            stage("docker image cleanup") {
                steps{
                    sh "docker rmi ${pipelineParams.dockerImageName}:${pipelineParams.dockerImageTag}"
                }
            }
        }
    }
}