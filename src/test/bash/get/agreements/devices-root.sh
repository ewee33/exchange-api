# Gets all agreements of device 1 as root
if [[ $1 == "-raw" ]]; then parse=cat; else parse="jq -r ."; fi
curl -# -w "%{http_code}" -X GET -H 'Accept: application/json' -H "Authorization:Basic root:$EXCHANGE_ROOTPW" $EXCHANGE_URL_ROOT/v1/devices/1/agreements | $parse