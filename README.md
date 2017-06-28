# GraphQLientPlugin

GraphQLient plugin ease communication between an *Android application* and a *GraphQL server*. \
It goes along with 
[Graphqlient library that can be found here](https://github.com/kelianClerc/graphlqlient_library).

This plugin takes a graphql request and generate two java classes from it : `QueryNameRequest` and 
`QueryNameResponse`\
The request file has request parameters as fields to ease dynamic use of the queries.\
 The response file contains the fields in which the response will be map.

## Requests\
### Where to store requests
At the root of your project create a `graphql` folder that will contain all the request you want to 
use.
The requests will be saved under `*.graphql extension.`

### Request Construction
`*.graphql` files contain valid graphql requests. You have to master Graphql specification to create
 Graphql request. You can find it [here](http://graphql.org/learn/)\
However, some improvements have been implemented in this plugin in order to generate more accurately 
classes
#### Field Typing
Because a graphql request is part of an HTTP request, their is no native variable typing in it. 
In order to manipulate the request in Java, we need type.

Before the field you want to describe, use the commentary : `#-type-` followed by one of the type of
the following list :
* `Boolean` 
* `Float` 
* `ID` 
* `Int`
* `String`
* `yourEnumQLEnum`

Example : 
```
{
  user(id:1) {
     #-type- String
     name,
     #-type- Boolean
     isVerifiedMember
  }
}
```
If you write inlines requests, please add `;` after the specified type to help the parser.

Example : 
`{user(id:1){#-type-String; name, #-type-Boolean; isVerifiedMember}}`

You don't have to specify `user` type. It is not a scalar type so it will be generated by the 
plugin.

#### Lists 
To specify that a field will hold a List, use the commentary `#-list-`
If you have to specify a list of scalar type, add the type after the commentary.

Example : 
```
{
    #-list-
     users {
        #-type- String
        name,
        #-list- Int
        someRandomIntList
     }
 }
```
If you write inlines requests, please add `;` after the specified type to help the parser.

Example : 
`{#-list-; users{#-type-String; name, #-list- Int; someRandomIntList}}`

### Request API

#### Name of the generated request
The request class generated will be named after the graphql query.\
For instance, with the query having the header : `query allUsersWithFriends(params) { fields }`
the class `AllUsersWithFriendsQLRequest` will be generated.

GraphQL request can be annonymous. In that case, the generated class will be named after the file
name.\
For instance, with the file `allUsers.graphql` containing `{users{name}}`, the plugin will generate 
the class `AllUsersQLRequest`.

If two requests have the same name, the second one computed will override the first one.

#### Use QLRequest object
To send a query, you need to pass an instance of the QLRequest you want to GraphQL's 
([graphqlient plugin](https://github.com/kelianClerc/graphlqlient_library)) method `send`

If your request has Query parameters, you can set the values of these parameters in the constructor 
of your QLRequest object.

Example :
We have the current .graphql file :
```
query userName($id: ID!) {
    user(id:$id) {
        name
    }
}

```
The plugin will generate the class `UserNameQLRequest` corresponding to it.
To instantiate it you will specifiy the id of user you need :
`UserNameQLRequest userRequest = new UserNameQLRequest(3)`

In the `.graphql` file, you can add a default value to a parameter not mandatory according to GraphQL
spec.\
If a mandatory field has not been set, `QLException` of `graphqlient` will be thrown when using the
request.

If you need it you can retrieve the string request that will be sent to the server with `userRequest.query()`

## Response
The plugin also generate a QLResponse class, named the same way that QLRequest class.
It has fields corresponding to the fields of the response. It also contains nested class 
corresponding to sub objects of the query. 
It will be mapped with the response from the GraphQL server by `graphqlient` library.

## Enum
Enum are considered as GraphQL scalar types. In order to use enums in query, you can create `*.qlenum` files containing the different values.
`GraphQLient Plugin` will generate an enum which can be used in your requests. It will be named with the following pattern :
`FileNameQLEnum`.

Example :
statuses.qlenum :
``` 
ONLINE, OFFLINE, IDLE
```

StatusesQLEnum.java :
```java
public enum StatusesQLEnum implements QLEnum {
  ONLINE("ONLINE"), OFFLINE("OFFLINE"), IDLE("IDLE");
  
  private final String id;

    private StatusesQLEnum(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
```

To use this enum in a `.graphql` file you must use `StatusesQLEnum`

## Todo
Todo

## Download
Add `jitpack` repository in project's `build.gradle`

```
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.kelianClerc:graphqlient_gradle_plugin:develop-SNAPSHOT'
    }
}

```

Add to module's `build.gradle` :
```
apply plugin: 'com.applidium.qlrequest'

#If you want to use GraphQLient library add this dependency
dependencies {
    compile "com.github.kelianClerc:graphqlient_library:develop-SNAPSHOT"
}
```
