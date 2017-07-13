pragma solidity ^0.4.11;

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "./EnglishAuctionOffering.sol";

contract EnglishAuctionOfferingFactory is OfferingFactory {

    function EnglishAuctionOfferingFactory(
        address ens,
        address offeringRegistry,
        address offeringRequests,
        address emergencyMultisig
    )
        OfferingFactory(ens, offeringRegistry, offeringRequests, emergencyMultisig)
    {
    }

    function createOffering(
        string name,
        uint startPrice,
        uint endTime,
        uint extensionDuration,
        uint minBidIncrease
    ) {
        var node = namehash(name);
        address newOffering = new EnglishAuctionOffering(
            offeringRegistry,
            ens,
            node,
            name,
            msg.sender,
            emergencyMultisig,
            startPrice,
            endTime,
            extensionDuration,
            minBidIncrease
        );

        registerOffering(node, newOffering, 100000);
    }
}

