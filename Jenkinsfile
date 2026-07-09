/*
 * Tier 1 of the two-tier parallelism model: the SHARD layer.
 *
 * The matrix below fans the build out into SHARD_TOTAL parallel branches.
 * Every branch checks out the same code and runs the same command inside the
 * official Playwright Java Docker image (JDK + browsers + OS deps
 * pre-installed) — the ONLY difference between branches is SHARD_INDEX.
 * JourneyManifest inside the JVM uses that to pick its disjoint slice of
 * journeys, then the JourneyEngine's ForkJoinPool (tier 2, the WORKER layer)
 * runs that slice concurrently.
 *
 * To go from 2 shards to 3: add '2' to the axis values and bump SHARD_TOTAL.
 * Nothing else changes — the manifest math redistributes the journeys.
 */
pipeline {
    agent none

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        SHARD_TOTAL = '2'
        WORKERS     = '2'      // ForkJoinPool threads per shard JVM
        HEADLESS    = 'true'
    }

    stages {
        stage('Journeys') {
            matrix {
                axes {
                    axis {
                        name 'SHARD_INDEX'
                        values '0', '1'
                    }
                }
                agent {
                    docker {
                        // Version pinned to match pom.xml's playwright.version
                        // so the pre-installed browsers are the right build.
                        image 'mcr.microsoft.com/playwright/java:v1.52.0-noble'
                        // -u root : avoid uid-mismatch permission issues in the container
                        // --ipc=host : recommended by Playwright for Chromium stability
                        args '-u root --ipc=host'
                    }
                }
                stages {
                    stage('Run shard') {
                        steps {
                            sh '''
                                echo "=== Shard ${SHARD_INDEX} of ${SHARD_TOTAL} — ${WORKERS} workers ==="
                                ./mvnw -B test -Dmaven.repo.local=.m2repo
                            '''
                        }
                    }
                }
                post {
                    always {
                        // Prefix artifacts with the shard so the two branches
                        // don't overwrite each other in the merged archive.
                        sh '''
                            rm -rf shard-${SHARD_INDEX} && mkdir -p shard-${SHARD_INDEX}
                            cp -r target/extent-report shard-${SHARD_INDEX}/ 2>/dev/null || true
                            cp -r target/traces        shard-${SHARD_INDEX}/ 2>/dev/null || true
                        '''
                        archiveArtifacts artifacts: "shard-${SHARD_INDEX}/**",
                                         allowEmptyArchive: true
                        junit testResults: 'target/surefire-reports/*.xml',
                              allowEmptyResults: true
                    }
                }
            }
        }
    }
}
