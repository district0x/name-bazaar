pragma solidity ^0.5.17;

/**
 * @title AuctionOfferingFactory
 * @dev Factory for creating new Auction offerings
 */

import "./OfferingRegistry.sol";
import "./OfferingFactory.sol";
import "./AuctionOffering.sol";
import "./Forwarder.sol";

contract AuctionOfferingFactory is OfferingFactory {

    constructor(
        ENSRegistry ens,
        OfferingRegistry offeringRegistry
    )
        public
        OfferingFactory(ens, offeringRegistry)
    {
    }

    /**
    * @dev Deploys new auction offering and registers it to OfferingRegistry
    * @param name string Plaintext ENS name
    * @param startPrice uint The start price of the auction
    * @param endTime uint The end time of the auction
    * @param extensionDuration uint The extension duration of the auction
    * @param minBidIncrease uint The min bid increase of the auction
    */
    function createSubnameOffering(
        // WARNING: The contract DOES NOT perform ENS name normalisation, which is up to responsibility of each offchain UI!
        string calldata name,
        uint startPrice,
        uint64 endTime,
        uint64 extensionDuration,
        uint minBidIncrease
    ) external {
        address payable forwarder = address(new Forwarder());
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

        registerSubnameOffering(node, labelHash, forwarder, version);
    }

    /**
    * @dev Deploys new TLD auction offering and registers it to OfferingRegistry
    * @param name string Plaintext ENS name
    * @param startPrice uint The start price of the auction
    * @param endTime uint The end time of the auction
    * @param extensionDuration uint The extension duration of the auction
    * @param minBidIncrease uint The min bid increase of the auction
    * @param originalOwner address Original owner of the name
    */
    function createTLDOffering(
        // WARNING: The contract DOES NOT perform ENS name normalisation, which is up to responsibility of each offchain UI!
        string memory name,
        uint startPrice,
        uint64 endTime,
        uint64 extensionDuration,
        uint minBidIncrease,
        address payable originalOwner
    ) internal {
        address payable forwarder = address(new Forwarder());
        bytes32 node = namehash(name);
        bytes32 labelHash = getLabelHash(name);
        uint128 version = 100001;                   // versioning for Auction offerings starts at number 100000

        AuctionOffering(forwarder).construct(
            node,
            name,
            labelHash,
            originalOwner,
            version,
            startPrice,
            endTime,
            extensionDuration,
            minBidIncrease
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
        (string memory name,
         uint startPrice,
         uint64 endTime,
         uint64 extensionDuration,
         uint minBidIncrease) = abi.decode(data, (string, uint, uint64, uint64, uint));
        address payable originalOwner = address(uint160(from));  // address to address payable in Solidity 0.5.x
        createTLDOffering(name, startPrice, endTime, extensionDuration, minBidIncrease, originalOwner);
        return this.onERC721Received.selector;
    }
}

