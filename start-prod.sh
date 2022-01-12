while true; do
  git pull
  ./gradlew clean shadowJar
  echo "------- Running bot -------"
  java -jar build/libs/ajSupport-*.jar "$1"
done