#!/usr/bin/env python
"""run_jenkins_job.py
Usage:
    run_jenkins_job.py --job <jobname> [--url <url>] [--parameters <data>] 

Examples:
    jenkins-trigger-console.py  --job deploy_my_app -u https://jenkins.example.com:8080 -p param1=abc,param2=develop
Options:
  -j, --job <jobname>               Job name.
  -u, --url <url>                   Jenkins URL [default: http://localhost:8080]
  -p, --parameters <data>           Comma separated job parameters i.e. a=1,b=2
"""

import requests
import json
import sys
import arparse
import urllib
from time import sleep


headers = {'content-type': 'application/json'}
auths_lookup = {
    'extnp' : ('username':'123456789000000000'),
    'ext'   : ('username':'13453423sfklj42342'),
}
ssl="/etc/pki/tls/cert.pem"
queue_poll_interval=2

class Trigger():
    def __init__(self, arguments):
        if arguments:
            try:
                self.url = arguments.url
                self.job = arguments.job
                self.auth = auths_lookup["extnp"] if ".extnp." in self.url else auths_lookup["ext"]
                self.parameters = {}
                self.parameters = dict(u.split("=") for u in arguments.parameters.split(","))
                params - urllib.urlencode(self.parameters)
                self.buidl_url = self.url + "/job/" + self.job + "/buildWithParameters?{}".format(params)
                print "Triggering a build. {}".format(self.build_url)
            except ValueError:
                print "Your parameters should be in key=value format separated by ; for multi value i.e. x=1,b=2"
                sys.exit(1)


    def trigger_build(self):
        
        # Do a build request
        build_request = requests.post(self.build_url, auth=self.auth, headers=headers, verify=ssl)
        if build_request.status_code == 201:
            query_url = build_request.headers['location'] + "api/json"
            print "Build is queued {} ".format(queue_url)
        else:
            error = build_request.json
            print "sBuild failed: {}".format(error)
            sys.exit(1)
        return queue_url
    

    def get_build_number(self, queue_url):
        # Poll till we get job number
        print ""
        print "Starting polling for our job to start"
        timer = 100

        wait_job_start = True 
        while wait_job_start:
            queue_request = requests.get(queue_url, auth=self.auth, headers=headers, verify=ssl)
            reason = queue_request.json()['why']
            if reason != None:
                print 
                print " . Waiting for job to start because of {}".format(reason)
                print
                timer -= 1
                sleep(queue_poll_interval)
            else:
                wait_job_start = False
                job_number = queue_request.json()['executable']['number']
                print " Job is being build number: ", job_number  

            if timer == 0:
                print " time out waiting for job to start"
                sys.exit(1)
        # Return the job numner of the working
        return job_number
    

    def check_status(self, job_number):
        # Get job console till job stops
        status_url = self.url + "/job/" + self.job + "/" + "{}".format(job_number) + "/api/json"
        timer = 100
        processing = True
        while processing:
            try:
                status_request = requests.get(status_url, auth=self.auth, headers=headers, verify=ssl)
                build_status = status_request.json()
                timer -= 1
                sleep(queue_poll_interval)
                if build_status['building']:
                    print "Job running in progress" if timer == 99 else ".",
                else:
                    if build_status['result'] == "SUCCESS":
                        print
                        print "Job {} has been completed with status {}.".format(self.job, build_status['result'])
                        processing = False
                    else:
                        print
                        print "Job {} has been failed with status {}.".format(self.job, build_status['result'])
                if timer == 0:
                    print "Time out waiting for job to end"
                    sys.exit(1)

            except requests.exceptions.RequestException as error:
                print "Failed to retrieve the status of the job. Error: {}".format(error)
                sys.exit(1)

    
    def main(self):
        queue_url = self.trigger_build()
        job_number = self.get_build_number(queue_url)
        self.check_status(job_number)

if __name__ == '__main__':
    parser = arparse.ArgumentParser(description='Trigger a Jenkins job build')
    parser.add_argument('--url', help='Jenkins URL', required=True)
    parser.add_argument('--job', help='Job name', required=True)
    parser.add_argument('--parameters', help='Commas separated job parameters i.e. a=1,b-2', required=True)
    arguments = parser.parse_args()
    jentrigger = Trigger(arguments)
    jentrigger.main()

