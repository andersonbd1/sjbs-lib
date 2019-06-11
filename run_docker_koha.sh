#docker run -d --name myKoha \
docker run --name myKoha \
	-p 6001:6001 -p 8080:8080 -p 8081:8081 \
	-e KOHA_INSTANCE=myKoha \
	-e KOHA_ADMINUSER=admin \
	-e KOHA_ADMINPASS=secret \
	-e SIP_WORKERS=3 \
	-e SIP_AUTOPASS1=autopass \
	-t digibib/koha
