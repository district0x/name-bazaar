// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4;

/**
 * @title UsedByFactories
 * @dev Provides modifiers to allow only offering factory contracts to execute method
 */

import "@openzeppelin/contracts/access/Ownable.sol";

contract UsedByFactories is Ownable {

    mapping(address => bool) public isFactory;

    modifier onlyFactory() {
        require(isFactory[msg.sender]);
        _;
    }

    function setFactories(address[] calldata factories, bool _isFactory)
        external
        onlyOwner
    {
        for(uint i = 0; i < factories.length; i++) {
            isFactory[factories[i]] = _isFactory;
        }
    }
}