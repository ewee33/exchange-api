# Updates user
if [[ $1 == "-raw" ]]; then parse=cat; else parse="jq -r ."; fi
curl -# -w "%{http_code}" -X PUT -H 'Content-Type: application/json' -H 'Accept: application/json' -H "Authorization:Basic $EXCHANGE_USER:$EXCHANGE_PW" -d '{
  "password": "'$EXCHANGE_PW'",
  "email": "'$EXCHANGE_USER'@hotmail.com"
}' $EXCHANGE_URL_ROOT/v1/users/$EXCHANGE_USER | $parse
