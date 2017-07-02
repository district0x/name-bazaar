#!/usr/bin/env bash
cd resources/public/contracts/src

solc --overwrite --optimize --bin --abi OfferingRegistry.sol -o ../build/
solc --overwrite --optimize --bin --abi InstantBuyOfferingFactory.sol -o ../build/
solc --overwrite --optimize --bin --abi EnglishAuctionOfferingFactory.sol -o ../build/
#solc --overwrite --optimize --bin --abi Test.sol -o ../build/
solc --overwrite --optimize --bin --abi District0xEmails.sol -o ../build/
solc --overwrite --optimize --bin --abi OfferingRequests.sol -o ../build/
solc --overwrite --optimize --bin --abi ens/ENS.sol -o ../build/
#solc --overwrite --optimize --bin --abi ens/FIFSRegistrar.sol -o ../build/

cd ../build
wc -c OfferingRegistry.bin | awk '{print "OfferingRegistry: " $1}'
wc -c InstantBuyOfferingFactory.bin | awk '{print "InstantBuyOfferingFactory: " $1}'
wc -c EnglishAuctionOfferingFactory.bin | awk '{print "EnglishAuctionOfferingFactory: " $1}'
#wc -c Test.bin | awk '{print "Test: " $1}'
wc -c OfferingRequests.bin | awk '{print "OfferingRequests: " $1}'
#wc -c FIFSRegistrar.bin | awk '{print "FIFSRegistrar: " $1}'
wc -c ENS.bin | awk '{print "ENS: " $1}'

