#!/usr/bin/env bash
cd resources/public/contracts/src

function solc-err-only {
    solc "$@" 2>&1 | grep -A 2 "Error"
}

solc-err-only --overwrite --optimize --bin --abi OfferingRegistry.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi BuyNowOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi AuctionOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi District0xEmails.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi OfferingRequests.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi MockRegistrar.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi ens/ENS.sol -o ../build/

#solc --overwrite --optimize --bin --abi Test.sol -o ../build/


cd ../build
wc -c OfferingRegistry.bin | awk '{print "OfferingRegistry: " $1}'
wc -c BuyNowOfferingFactory.bin | awk '{print "BuyNowOfferingFactory: " $1}'
wc -c BuyNowOffering.bin | awk '{print "BuyNowOffering: " $1}'
wc -c BuyNowOfferingLibrary.bin | awk '{print "BuyNowOfferingLibrary: " $1}'
wc -c AuctionOfferingFactory.bin | awk '{print "AuctionOfferingFactory: " $1}'
wc -c AuctionOffering.bin | awk '{print "AuctionOffering: " $1}'
wc -c AuctionOfferingLibrary.bin | awk '{print "AuctionOfferingLibrary: " $1}'
wc -c OfferingRequests.bin | awk '{print "OfferingRequests: " $1}'

#wc -c Test.bin | awk '{print "Test: " $1}'

