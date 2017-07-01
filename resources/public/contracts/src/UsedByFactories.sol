pragma solidity ^0.4.11;

import "Ownable.sol";

contract UsedByFactories is Ownable {

    mapping(address => bool) public isFactory;

    modifier onlyFactory() {
        require(isFactory[msg.sender]);
        _;
    }

    function setFactories(address[] factories, bool _isFactory)
        onlyOwner
    {
        for(uint i = 0; i < factories.length; i++) {
            isFactory[factories[i]] = _isFactory;
        }
    }
}