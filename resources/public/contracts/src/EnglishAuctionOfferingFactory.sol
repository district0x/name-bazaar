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
}

