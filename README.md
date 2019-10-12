# run_jenkins_job
Trigger a Jenkins job via API

Usage:
    run_jenkins_job.py --job <jobname> [--url <url>] [--parameters <data>]

Examples:
    jenkins-trigger-console.py  --job deploy_my_app -u https://jenkins.example.com:8080 -p param1=abc,param2=develop
Options:
  -j, --job <jobname>               Job name.
  -u, --url <url>                   Jenkins URL [default: http://localhost:8080]
  -p, --parameters <data>           Comma separated job parameters i.e. a=1,b=2
