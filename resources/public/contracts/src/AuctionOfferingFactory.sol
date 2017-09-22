pragma solidity ^0.4.14;

/**
 * @title OfferingFactory
 * @dev Factory for creating new Auction offerings
 */

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

    /**
    * @dev Deploys new Auction offering and registers it to OfferingRegistry
    * @param name string Plaintext ENS name
    * @param startPrice uint The start price of the auction
    * @param endTime uint The end time of the auction
    * @param extensionDuration uint The extension duration of the auction
    * @param minBidIncrease uint The min bid increase of the auction
    */
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

        registerOffering(node, labelHash, newOffering, 100000); // versioning for Auction offerings starts at number 100000
    }
}

