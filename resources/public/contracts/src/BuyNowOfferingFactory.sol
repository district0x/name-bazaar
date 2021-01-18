pragma solidity ^0.5.0;

/**
 * @title BuyNowOfferingFactory
 * @dev Factory for creating new BuyNow offerings
 */

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "BuyNowOffering.sol";
import "Forwarder.sol";

contract BuyNowOfferingFactory is OfferingFactory {

    constructor(
        ENSRegistry ens,
        OfferingRegistry offeringRegistry,
        OfferingRequestsAbstract offeringRequests
    )
        public
        OfferingFactory(ens, offeringRegistry, offeringRequests)
    {
    }

    /**
    * @dev Deploys new BuyNow offering and registers it to OfferingRegistry
    * @param name string Plaintext ENS name
    * @param price uint The price of the offering
    */
    function createOffering(
        // WARNING: The contract DOES NOT perform ENS name normalisation, which is up to responsibility of each offchain UI!
        string calldata name,
        uint price
    ) external {
        bytes32 node = namehash(name);
        bytes32 labelHash = getLabelHash(name);
        address payable forwarder = address(new Forwarder());
        uint128 version = 2; // versioning for BuyNow offerings starts at number 1

        BuyNowOffering(forwarder).construct(
            node,
            name,
            getLabelHash(name),
            msg.sender,
            version,
            price
        );

        registerOffering(node, labelHash, forwarder, version);
    }
}

