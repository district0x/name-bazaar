pragma solidity ^0.5.17;

/**
 * @title BuyNowOfferingFactory
 * @dev Factory for creating new BuyNow offerings
 */

import "./OfferingRegistry.sol";
import "./OfferingFactory.sol";
import "./BuyNowOffering.sol";
import "./Forwarder.sol";

contract BuyNowOfferingFactory is OfferingFactory {

    constructor(
        ENSRegistry ens,
        OfferingRegistry offeringRegistry
    )
        public
        OfferingFactory(ens, offeringRegistry)
    {
    }

    /**
    * @dev Deploys new BuyNow offering and registers it to OfferingRegistry
    * @param name string Plaintext ENS name
    * @param price uint The price of the offering
    */
    function createSubnameOffering(
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
            labelHash,
            msg.sender,
            version,
            price
        );

        registerSubnameOffering(node, labelHash, forwarder, version);
    }

    /**
   * @dev Deploys new BuyNow offering for TLD, registers it and gives it name ownership
   * @param name string Plaintext ENS name
   * @param price uint The price of the offering
   * @param originalOwner address Original owner of the name
   */
    function createTLDOffering(
        // WARNING: The contract DOES NOT perform ENS name normalisation, which is up to responsibility of each offchain UI!
        string memory name,
        uint price,
        address payable originalOwner
    ) internal {
        bytes32 node = namehash(name);
        bytes32 labelHash = getLabelHash(name);
        address payable forwarder = address(new Forwarder());
        uint128 version = 2; // versioning for BuyNow offerings starts at number 1

        BuyNowOffering(forwarder).construct(
            node,
            name,
            labelHash,
            originalOwner,
            version,
            price
        );

        registerTLDOffering(node, labelHash, forwarder, version, originalOwner);
    }

    /**
    * @dev Recognizes being sent .eth subdomain ownership via EthRegistrar and initiates offering creation
    * @param operator address Who initiated the transfer
    * @param from address Original owner of the domain
    * @param tokenId uint256 EthRegistrar identifier of the domain
    * @param data bytes memory Additional data sent with transfer encoding the desired offering parameters
    */
    function onERC721Received(address operator, address from, uint256 tokenId, bytes memory data)
        public
        onlyRegistrar
        returns (bytes4)
    {
        (string memory name, uint price) = abi.decode(data, (string, uint));
        address payable originalOwner = address(uint160(from));  // address to address payable in Solidity 0.5.x
        createTLDOffering(name, price, originalOwner);
        return this.onERC721Received.selector;
    }
}

