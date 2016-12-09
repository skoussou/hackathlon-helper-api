# Hackathlon Helper API
helper-api microservice using Java EE (JAX-RS) on WildFly Swarm

The detailed instructions to run *Red Hat Helloworld MSA* demo, can be found at the following repository: <https://github.com/redhat-helloworld-msa/helloworld-msa>


Build and Deploy hola locally
-----------------------------

1. Open a command prompt and navigate to the root directory of this microservice.
2. Type this command to build and execute the application:

        mvn wildfly-swarm:run

3. This will create a uber jar at  `target/hola-swarm.jar` and execute it.
4. The application will be running at the following URL: <http://localhost:8080/api>
5. To see the APIs go to the following URL: <http://localhost:8080/api/swagger.json>

Deploy the application in Openshift
-----------------------------------

1. Make sure to be connected to the Docker Daemon
2. Execute

		mvn clean package docker:build fabric8:json fabric8:apply

Alternative
-----------------------------------
1. Add WildflySwarm Template

		Login as admin user on OCP (cannot be done as normal non-admin user) 	: oc login https://35.156.133.70:8443
		Go to project openshift							: oc project openshift
		Add the widflyswarm template						: oc create -f https://raw.githubusercontent.com/wildfly-swarm/sti-wildflyswarm/master/1.0/wildflyswarm-sti-all.json
		Check progress								: oc logs bc/wildflyswarm-10-centos7-build -f
		Should be in the templates (if not in cli it is in console)		: oc get templates -n openshift

2. Go to OCP Project/Namespace where this will be exposed from
3. Create new img		

		oc new-app --name helper-api wildflyswarm-10-centos7~https://github.com/skoussou/hackathlon-helper-api

4. Check progress with: 	

		oc status & oc logs -f bc/helper-api

5. if things go wrong you probably have to delete the following (check if they exist)

		 - oc delete imagestream helper-api
		 - oc delete buildconfig helper-api
		 - oc delete deploymentconfigs helper-api
		 - oc delete deploymentconfig helper-api
		 - oc delete service helper-api
		
		 - and redo the above
6. Add route
7. Don't forget to add to /etc/hosts against infra IP if needed to call from browser or external to OCP if your OCP cluster doesn't resolve via DNS the route


