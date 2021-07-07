#!groovy

pipeline {
    agent any
    environment {
        TFE_TOKEN = credentials('TFE_Token')
        TERRAFORM_RC_PATH = "${HOME}/.terraformrc"
        TERRAFORM_RC_CONFIG_FILE_ID = 'xxxxx-xxxxx-xxxxx-xxxxx'
    }
    options {
        timestamps ()
    }

    stages {
        stage('setup') {
            script {
                module_name = env.TFE_Module
                AWS_ACCESS_KEY_ID = env.AWS_ACCESS_KEY_ID
                AWS_SECRET_ACCESS_KEY = env.AWS_SECRET_ACCESS_KEY
                AWS_SECURITY_TOKEN = env.AWS_SECURITY_TOKEN

                go_root = tool(type: 'go', name: '1.16.4')
                env.GOROOT = go_root
                env.PATH = "${go_root}/bin:${env.PATH}"
                terraform_root = tool(type: 'terraform', name: '0.13.7')
                env.PATH = "${terraform_root}:${env.PATH}"
                currentBuild.display = "#${BUILD_NUMBER} - ${env.TFE_Module}"

                String varenviron = "hostname".execute().text
                varenviron = varenviron.replaceAll(".liz.com","").substring(varenviron.indexOf(".")+1).replaceAll("\\n","");
                varenviron.trim();
                if ( varenviron == "dev" ) {
                    sshAgent = "yyyyyyy-yyy-yyyy-yyyyyy-yyyyy";
                }
                else if ( varenviron == "prod"){
                     sshAgent = "zzzzz-zzzz-zzzz-zzzz-zzzzz";
                }
                
                // Define the Terraform CLI Configuration File with API Token Defined
                configFileProvider([configFile(fileId: env.TERRAFORM_RC_PATH, replaceTokens: true, targetLocation: env.TERRAFORM_RC_PATH)]){ 
                }
            }
        }
        stage('checkout module') {
            steps {
                script {
                    sshagent(["${sshAgent}"]){
                        println "Checking out the module"
                        sh script: '''
                            rm -r terraform-aws-'''+module_name+'''
                            rm -r terraform-testing
                            git clone git@github.com:terraform-modules/terraform-aws-'''+module_name+'''.git
                            git clone git@github.com:Liz/terraform-testing.git
                        '''
                    }
                }
            }
        }
        stage('execute') {
            steps {
                sh '''
                    export AWS_ACCESS_KEY_ID='''+AWS_ACCESS_KEY_ID+'''
                    export AWS_SECRET_ACCESS_KEY='''+AWS_SECRET_ACCESS_KEY+'''
                    export AWS_SECURITY_TOKEN='''+AWS_SECURITY_TOKEN+'''
                    cd terraform-aws-'''+module_name+'''/test/

                    # Terratest Releases: https://github.com/gruntwork-io/terratest/releases
                    TERRATEST_RELEASE="v0.35.1"
                    LOG_PARSER_PATH="/var/lib/jenkins/tools/terratest_log_parser/$TERRATEST_RELEASE"

                    ! [[ -d $LOG_PARSER_PATH ]] && mkdir -p $LOG_PARSER_PATH

                    ! [[ -f "$LOG_PARSER_PATH/terratest_log_parser" ]] &&
                    curl --location --silent --fail --show-error -o terratest_log_parser "https://github.com/gruntwork-io/terratest/releases/download/$TERRATEST_RELEASE/terratest_log_parser_linux_amd64" &&
                    chmod +x terratest_log_parser &&
                    mv terratest_log_parser $LOG_PARSER_PATH

                    export PATH=$LOG_PARSER_PATH:$PATH

                    go test -v -timeout 15m -run . | tee test_output.log

                    ! [[ -d "test_output" ]] && mkdir "test_output"
                    terratest_log_parser -testlog test_output.log -outputdir test_output
                '''
            }
        }
        stage('results') {
            steps {
                junit 'test/test_output/report.xml'
            }
        }
    }
