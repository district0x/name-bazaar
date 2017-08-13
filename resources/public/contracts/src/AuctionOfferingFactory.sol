pragma solidity ^0.4.14;


import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "AuctionOffering.sol";


contract AuctionOfferingFactory is OfferingFactory {

    function AuctionOfferingFactory(
    address registrar,
    address offeringRegistry,
    address offeringRequests,
    address emergencyMultisig
    )
    OfferingFactory(registrar, offeringRegistry, offeringRequests, emergencyMultisig)
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
        var labelHash = getLabelHash(name);
        address newOffering = new AuctionOffering(
        offeringRegistry,
        registrar,
        node,
        name,
        labelHash,
        msg.sender,
        emergencyMultisig,
        startPrice,
        endTime,
        extensionDuration,
        minBidIncrease
        );

        registerOffering(node, labelHash, newOffering, 100000);
    }
}

