def call(Map config = [:]) {
    def jobName = config.jobName
    if (jobName == null || jobName.isEmpty()) {
        throw new RuntimeException("The jobName argument must be provided to k8sLocalDeployment()");
    }

    pipeline {
        agent { label config.workerAgent ?: 'Jenkins-Default-Agent-X64-C5xlarge-Single-Host' }

        parameters {
            string(name: 'GIT_REPO_URL', defaultValue: 'https://github.com/opensearch-project/opensearch-migrations.git', description: 'Git repository url')
            string(name: 'GIT_BRANCH', defaultValue: 'main', description: 'Git branch to use for repository')
        }

        options {
            timeout(time: 1, unit: 'HOURS')
            buildDiscarder(logRotator(daysToKeepStr: '30'))
        }

        triggers {
            GenericTrigger(
                    genericVariables: [
                            [key: 'GIT_REPO_URL', value: '$.GIT_REPO_URL'],
                            [key: 'GIT_BRANCH', value: '$.GIT_BRANCH'],
                            [key: 'job_name', value: '$.job_name']
                    ],
                    tokenCredentialId: 'jenkins-migrations-generic-webhook-token',
                    causeString: 'Triggered by PR on opensearch-migrations repository',
                    regexpFilterExpression: "^$jobName\$",
                    regexpFilterText: "\$job_name",
            )
        }

        stages {
            stage('Checkout') {
                steps {
                    script {
                        git branch: "${params.GIT_BRANCH}", url: "${params.GIT_REPO_URL}"
                    }
                }
            }

            stage('Check Minikube Status') {
                steps {
                    timeout(time: 1, unit: 'MINUTES') {
                        script {
                            def status = sh(script: "minikube status --format='{{.Host}}'", returnStdout: true).trim()
                            if (status == "Running") {
                                echo "✅ Minikube is running"
                            } else {
                                error("❌ Minikube is NOT running")
                            }
                        }
                    }
                }
            }

            stage('Build Docker Images (Minikube)') {
                steps {
                    timeout(time: 30, unit: 'MINUTES') {
                        dir('deployment/k8s') {
                            script {
                                sh "sudo -u ec2-user ./buildDockerImagesMini.sh"
                            }
                        }
                    }
                }
            }

            stage('Perform Python E2E Tests') {
                steps {
                    timeout(time: 15, unit: 'MINUTES') {
                        dir('libraries/testAutomation') {
                            script {
                                sh "sudo -u ec2-user pipenv install --deploy"
                                sh "sudo -u ec2-user pipenv run app --skip-delete"
                            }
                        }
                    }
                }
            }
        }
        post {
            always {
                timeout(time: 15, unit: 'MINUTES') {
                    dir('libraries/testAutomation') {
                        script {
                            sh "sudo -u ec2-user pipenv install --deploy"
                            sh "sudo -u ec2-user pipenv run app --delete-only"
                        }
                    }
                }
            }
        }
    }
}
