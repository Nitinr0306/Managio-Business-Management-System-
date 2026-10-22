// Managio pipeline (D-031): test both apps, build and push immutable images, then release in order:
// API image to Render, wait until it is ready, Vercel production build of the web app, then smoke-test the site.
//
// Runs on a Linux or a Windows agent. The agent needs Docker (the API tests use Testcontainers, and the images are
// built here), Node 22 and curl. Configuration lives in Jenkins, never in this file (docs/DEPLOYMENT.md, "Jenkins"):
//   credentials  dockerhub-token     Docker Hub access token for DOCKER_USERNAME (Secret text)
//                render-deploy-hook  Render deploy hook URL of the API service (Secret text)
//                vercel-deploy-hook  Vercel deploy hook URL for the master branch (Secret text)
//   parameters   API_URL, WEB_URL, NOTIFY_EMAIL, ROLLBACK_TO_TAG (below)
// Deploying happens only for the master branch; every other branch is tested and built.

/** Runs a command in the agent's shell: sh on Linux and macOS, cmd on Windows. */
def run(String unixCommand, String windowsCommand) {
    if (isUnix()) {
        sh unixCommand
    } else {
        bat windowsCommand
    }
}

/** The HTTP status of a GET to {@code url}, or "000" when it can't be reached. */
def httpStatus(String url) {
    def status = isUnix()
            ? sh(returnStdout: true, script: "curl -s -o /dev/null -w '%{http_code}' --max-time 20 '${url}' || true")
            : bat(returnStdout: true, script: "@curl.exe -s -o NUL -w \"%%{http_code}\" --max-time 20 \"${url}\"")
    return status.trim()
}

/** Polls {@code url} until it answers 200 or {@code minutes} pass. */
def waitForOk(String url, int minutes) {
    timeout(time: minutes, unit: 'MINUTES') {
        waitUntil(initialRecurrencePeriod: 15000, quiet: true) {
            def status = httpStatus(url)
            echo "${url} -> ${status}"
            return status == '200'
        }
    }
}

def isMaster() {
    return (env.GIT_BRANCH ?: '').replaceFirst('^origin/', '') == 'master'
}

pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    // A Jenkins on a laptop can't receive GitHub webhooks, so it polls. With a public Jenkins, add a GitHub webhook
    // to <jenkins>/github-webhook/ and this still works as a fallback.
    triggers {
        pollSCM('H/5 * * * *')
    }

    parameters {
        string(name: 'API_URL', defaultValue: '', description: 'The API on Render, e.g. https://managio-api.onrender.com (no trailing slash). Needed to deploy.')
        string(name: 'WEB_URL', defaultValue: 'https://managio.me', description: 'The public web app.')
        string(name: 'NOTIFY_EMAIL', defaultValue: '', description: 'Who hears about failed builds (needs Jenkins mail settings).')
        string(name: 'ROLLBACK_TO_TAG', defaultValue: '', description: 'Redeploy this earlier API image tag instead of building (e.g. 1a2b3c4d-41). Roll the web app back in Vercel.')
    }

    environment {
        DOCKER_USERNAME = 'nitinr1103'
        API_IMAGE = 'managio-backend'
        WEB_IMAGE = 'managio-frontend'
        // The web image is for self-hosting (Vercel builds from source): its /api pass-through goes to the Compose
        // service name. The Google client ID is public by design.
        WEB_IMAGE_API_ORIGIN = 'http://api:8080'
    }

    stages {

        stage('Version') {
            steps {
                script {
                    env.TAG = params.ROLLBACK_TO_TAG?.trim() ?: "${env.GIT_COMMIT.take(8)}-${env.BUILD_NUMBER}"
                    env.API_IMAGE_REF = "docker.io/${env.DOCKER_USERNAME}/${env.API_IMAGE}:${env.TAG}"
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.TAG}${params.ROLLBACK_TO_TAG?.trim() ? ' (rollback)' : ''}"
                }
            }
        }

        stage('Test API') {
            when { expression { !params.ROLLBACK_TO_TAG?.trim() } }
            steps {
                dir('managio_backend') {
                    run('chmod +x mvnw && ./mvnw -B verify', 'mvnw.cmd -B verify')
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'managio_backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Check web app') {
            when { expression { !params.ROLLBACK_TO_TAG?.trim() } }
            steps {
                dir('managio_frontend') {
                    run('npm ci --no-audit --no-fund', 'npm ci --no-audit --no-fund')
                    run('npm run typecheck && npm run lint && npm test', 'npm run typecheck && npm run lint && npm test')
                }
            }
        }

        stage('Build images') {
            when { expression { !params.ROLLBACK_TO_TAG?.trim() } }
            steps {
                run('docker build -t "$DOCKER_USERNAME/$API_IMAGE:$TAG" ./managio_backend',
                    'docker build -t "%DOCKER_USERNAME%/%API_IMAGE%:%TAG%" ./managio_backend')
                run('docker build --build-arg API_ORIGIN="$WEB_IMAGE_API_ORIGIN" -t "$DOCKER_USERNAME/$WEB_IMAGE:$TAG" ./managio_frontend',
                    'docker build --build-arg API_ORIGIN="%WEB_IMAGE_API_ORIGIN%" -t "%DOCKER_USERNAME%/%WEB_IMAGE%:%TAG%" ./managio_frontend')
            }
        }

        stage('Push images') {
            when { expression { isMaster() && !params.ROLLBACK_TO_TAG?.trim() } }
            steps {
                withCredentials([string(credentialsId: 'dockerhub-token', variable: 'DOCKER_TOKEN')]) {
                    run('echo "$DOCKER_TOKEN" | docker login -u "$DOCKER_USERNAME" --password-stdin',
                        'echo %DOCKER_TOKEN%| docker login -u %DOCKER_USERNAME% --password-stdin')
                }
                // Immutable tags are what gets deployed; latest only marks the newest build.
                run('docker push "$DOCKER_USERNAME/$API_IMAGE:$TAG" && docker push "$DOCKER_USERNAME/$WEB_IMAGE:$TAG"',
                    'docker push "%DOCKER_USERNAME%/%API_IMAGE%:%TAG%" && docker push "%DOCKER_USERNAME%/%WEB_IMAGE%:%TAG%"')
                run('docker tag "$DOCKER_USERNAME/$API_IMAGE:$TAG" "$DOCKER_USERNAME/$API_IMAGE:latest" && docker push "$DOCKER_USERNAME/$API_IMAGE:latest" && docker tag "$DOCKER_USERNAME/$WEB_IMAGE:$TAG" "$DOCKER_USERNAME/$WEB_IMAGE:latest" && docker push "$DOCKER_USERNAME/$WEB_IMAGE:latest"',
                    'docker tag "%DOCKER_USERNAME%/%API_IMAGE%:%TAG%" "%DOCKER_USERNAME%/%API_IMAGE%:latest" && docker push "%DOCKER_USERNAME%/%API_IMAGE%:latest" && docker tag "%DOCKER_USERNAME%/%WEB_IMAGE%:%TAG%" "%DOCKER_USERNAME%/%WEB_IMAGE%:latest" && docker push "%DOCKER_USERNAME%/%WEB_IMAGE%:latest"')
            }
        }

        stage('Deploy API (Render)') {
            when { expression { isMaster() } }
            steps {
                script {
                    if (!params.API_URL?.trim()) {
                        error('Set the API_URL parameter (the Render URL of the API) to deploy.')
                    }
                }
                // Render runs the exact image tested above; Flyway applies new migrations when it starts.
                withCredentials([string(credentialsId: 'render-deploy-hook', variable: 'RENDER_HOOK')]) {
                    run('curl -fsS -X POST "$RENDER_HOOK&imgURL=$API_IMAGE_REF" -o /dev/null',
                        'curl.exe -fsS -X POST "%RENDER_HOOK%&imgURL=%API_IMAGE_REF%" -o NUL')
                }
                // Give Render time to start replacing the running instance before polling it.
                sleep(time: 60, unit: 'SECONDS')
                waitForOk("${params.API_URL.trim()}/actuator/health/readiness", 15)
            }
        }

        stage('Deploy web app (Vercel)') {
            when { expression { isMaster() && !params.ROLLBACK_TO_TAG?.trim() } }
            steps {
                // Vercel builds this commit of master with the project's production settings. Pushes alone don't
                // deploy production (managio_frontend/vercel.json), so the web app never runs ahead of the API.
                withCredentials([string(credentialsId: 'vercel-deploy-hook', variable: 'VERCEL_HOOK')]) {
                    run('curl -fsS -X POST "$VERCEL_HOOK" -o /dev/null', 'curl.exe -fsS -X POST "%VERCEL_HOOK%" -o NUL')
                }
                sleep(time: 90, unit: 'SECONDS')
                waitForOk("${params.WEB_URL.trim()}/sign-in", 15)
            }
        }

        stage('Smoke test') {
            when { expression { isMaster() } }
            steps {
                script {
                    def web = params.WEB_URL.trim()
                    def checks = [
                            ["${web}/sign-in", '200'],
                            // Through the web app's /api pass-through to the API: no session, so 401.
                            ["${web}/api/v1/me", '401'],
                            // The API description is never published in production.
                            ["${params.API_URL.trim()}/v3/api-docs", '404'],
                    ]
                    checks.each { url, expected ->
                        def status = httpStatus(url)
                        echo "${url} -> ${status} (expected ${expected})"
                        if (status != expected) {
                            error("Smoke test failed: ${url} answered ${status}, expected ${expected}. Roll back (docs/DEPLOYMENT.md).")
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (isUnix()) {
                    sh 'docker logout || true'
                } else {
                    bat 'docker logout || exit /b 0'
                }
            }
        }
        failure {
            script {
                if (params.NOTIFY_EMAIL?.trim()) {
                    try {
                        mail to: params.NOTIFY_EMAIL.trim(),
                             subject: "Managio build ${currentBuild.displayName} failed",
                             body: "Stage results and logs: ${env.BUILD_URL}\nRollback: docs/DEPLOYMENT.md, \"Rollback\"."
                    } catch (e) {
                        echo "Could not send the failure email (configure mail in Manage Jenkins): ${e.message}"
                    }
                }
            }
        }
    }
}
