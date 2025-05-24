gradle \
-Dhttps.proxyHost=proxy.aexp.com \
-Dhttps.proxyPort=8080 \
-DsystemProp.https.proxyUser=$PROXY_USERNAME \
-Dhttps.proxyPassword=$PROXY_PASSWORD \
wrapper --gradle-version latest --stacktrace
