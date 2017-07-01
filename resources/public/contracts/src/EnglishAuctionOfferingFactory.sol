pragma solidity ^0.4.11;

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "EnglishAuctionOffering.sol";

contract EnglishAuctionOfferingFactory is OfferingFactory {

    function EnglishAuctionOfferingFactory(address ens, address offeringRegistry, address offeringRequests)
        OfferingFactory(ens, offeringRegistry, offeringRequests)
    {
    }

    function createOffering(
        string name,
        uint startPrice,
        uint startTime,
        uint endTime,
        uint extensionDuration,
        uint extensionTriggerDuration,
        uint minBidIncrease
    ) {
        var node = namehash(name);
        address newOffering = new EnglishAuctionOffering(ens, node, name, msg.sender, startPrice, startTime,
            endTime, extensionDuration, extensionTriggerDuration, minBidIncrease);

        registerOffering(node, newOffering);
    }

//    // If this function was in OfferingFactory it'd be throwing error
//    function namehash(string name) constant returns(bytes32) {
//        var nameSlice = name.toSlice();
//        var delim = ".".toSlice();
//        var labels = new string[](nameSlice.count(delim) + 1);
//        var node = bytes32(0);
//
//        if (nameSlice.len() > 0) {
//            for(uint i = 0; i < labels.length; i++) {
//                labels[i] = nameSlice.split(delim).toString();
//            }
//
//            for(int j = int(labels.length) - 1; j >= 0; j--) {
//                var labelSha = sha3(labels[uint(j)]);
//                node = sha3(node, labelSha);
//            }
//        }
//
//        return node;
//    }
}

