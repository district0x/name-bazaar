pragma solidity ^0.5.17;

import "@ensdomains/ens/contracts/ENSRegistry.sol";
import "@ensdomains/ethregistrar/contracts/BaseRegistrar.sol";
import "openzeppelin-solidity/contracts/token/ERC721/IERC721Receiver.sol";
import "./OfferingRegistry.sol";
import "./strings.sol";

/**
 * @title OfferingFactory
 * @dev Base contract factory for creating new offerings
 */

contract OfferingFactory is IERC721Receiver {
    using strings for *;

    ENSRegistry public ens;
    OfferingRegistry public offeringRegistry;

    // Hardcoded namehash of "eth"
    bytes32 public constant rootNode = 0x93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae;

    /**
     * @dev Modifier to make a function callable only by the ENS EthRegistrar
     */
    modifier onlyRegistrar {
        require(msg.sender == ens.owner(rootNode));
        _;
    }

    constructor(
        ENSRegistry _ens,
        OfferingRegistry _offeringRegistry
    ) public {
        ens = _ens;
        offeringRegistry = _offeringRegistry;
    }

    /**
    * @dev Registers new subname offering to OfferingRegistry
    * Must check if creator of offering is actual owner of ENS name
    * @param node bytes32 ENS node
    * @param labelHash bytes32 ENS labelhash
    * @param newOffering address The address of new offering
    * @param version uint The version of offering contract
    */
    function registerSubnameOffering(bytes32 node, bytes32 labelHash, address newOffering, uint version)
        internal
    {
        require(ens.owner(node) == msg.sender);
        offeringRegistry.addOffering(newOffering, node, msg.sender, version);
    }

    /**
    * @dev Registers new top level name offering to OfferingRegistry
    * Checks we own the name already and passes it to the offering
    * @param node bytes32 ENS node
    * @param labelHash bytes32 ENS labelhash
    * @param newOffering address The address of new offering
    * @param version uint The version of offering contract
    * @param originalOwner address The original owner of the name
    */
    function registerTLDOffering(
        bytes32 node,
        bytes32 labelHash,
        address newOffering,
        uint version,
        address originalOwner
    )
        internal
    {
        require(node == keccak256(abi.encodePacked(rootNode, labelHash)));
        BaseRegistrar registrar = BaseRegistrar(ens.owner(rootNode));
        uint256 tokenId = uint256(labelHash);
        address tokenOwner = registrar.ownerOf(tokenId);
        require(msg.sender == address(registrar) && tokenOwner == address(this));

        registrar.reclaim(tokenId, newOffering);
        registrar.transferFrom(tokenOwner, newOffering, tokenId);
        offeringRegistry.addOffering(newOffering, node, originalOwner, version);
    }

    /**
    * @dev Namehash function used by ENS to convert plain text name into node hash
    * @param name string Plaintext ENS name
    * @return bytes32 ENS node hash, aka node
    */
    function namehash(string memory name) internal returns(bytes32) {
        strings.slice memory nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        bytes memory label = abi.encodePacked(nameSlice.split(".".toSlice()).toString());
        return keccak256(abi.encodePacked(namehash(nameSlice.toString()), keccak256(label)));
    }

    /**
    * @dev Calculates labelHash of ENS name
    * @param name string Plaintext ENS name
    * @return bytes32 ENS labelHash, hashed label of the name
    */
    function getLabelHash(string memory name) internal returns(bytes32) {
        return keccak256(abi.encodePacked(name.toSlice().split(".".toSlice()).toString()));
    }
}