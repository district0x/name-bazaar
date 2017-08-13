#!/usr/bin/env bash
cd resources/public/contracts/src

function solc-err-only {
    solc "$@" 2>&1 | grep -A 2 "Error"
}

solc-err-only --overwrite --optimize --bin --abi OfferingRegistry.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi InstantBuyOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi EnglishAuctionOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi District0xEmails.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi OfferingRequests.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi MockRegistrar.sol -o ../build/

#solc --overwrite --optimize --bin --abi Test.sol -o ../build/


cd ../build
wc -c OfferingRegistry.bin | awk '{print "OfferingRegistry: " $1}'
wc -c InstantBuyOfferingFactory.bin | awk '{print "InstantBuyOfferingFactory: " $1}'
wc -c InstantBuyOffering.bin | awk '{print "InstantBuyOffering: " $1}'
wc -c InstantBuyOfferingLibrary.bin | awk '{print "InstantBuyOfferingLibrary: " $1}'
wc -c EnglishAuctionOfferingFactory.bin | awk '{print "EnglishAuctionOfferingFactory: " $1}'
wc -c EnglishAuctionOffering.bin | awk '{print "EnglishAuctionOffering: " $1}'
wc -c EnglishAuctionOfferingLibrary.bin | awk '{print "EnglishAuctionOfferingLibrary: " $1}'
wc -c OfferingRequests.bin | awk '{print "OfferingRequests: " $1}'
wc -c ENS.bin | awk '{print "ENS: " $1}'

#wc -c Test.bin | awk '{print "Test: " $1}'

