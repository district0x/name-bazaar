pragma solidity ^0.4.18;

/**
 * @title AuctionOfferingFactory
 * @dev Factory for creating new Auction offerings
 */

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "AuctionOffering.sol";
import "Forwarder.sol";

contract AuctionOfferingFactory is OfferingFactory {

    constructor(
        ENSRegistry ens,
        OfferingRegistry offeringRegistry,
        OfferingRequestsAbstract offeringRequests
    )
    OfferingFactory(ens, offeringRegistry, offeringRequests)
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
        // WARNING: The contract DOES NOT perform ENS name normalisation, which is up to responsibility of each offchain UI!
        string name,
        uint startPrice,
        uint64 endTime,
        uint64 extensionDuration,
        uint minBidIncrease
    ) {
        address forwarder = address(new Forwarder());
        bytes32 node = namehash(name);
        bytes32 labelHash = getLabelHash(name);
        uint128 version = 100001;                   // versioning for Auction offerings starts at number 100000

        AuctionOffering(forwarder).construct(
            node,
            name,
            labelHash,
            msg.sender,
            version,
            startPrice,
            endTime,
            extensionDuration,
            minBidIncrease
        );

        registerOffering(node, labelHash, forwarder, version);
    }
}

