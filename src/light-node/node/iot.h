/* -*- mode: c; coding: utf-8 -*- */

#include <IceC/IceC.h>

typedef struct {
    Ice_Int size;
    Ice_Byte* items;
} IDM_Address;

typedef IDM_Address* IDM_AddressPtr;

static void
IDM_Address_writeToOutputStream(IDM_AddressPtr self,
                                Ice_OutputStreamPtr os) {
    int i = 0;

    trace();
    
    Ptr_check(os);
    
    Ice_OutputStream_writeSize(os, self->size);
    
    for (i=0; i<self->size; i++) {
        Ice_OutputStream_writeByte(os, *(self->items + i));
    }
}

static void
IDM_Address_readFromInputStream(IDM_AddressPtr self,
                                Ice_InputStreamPtr is) {

    trace();
    
    Ptr_check(is);
    
    Ice_InputStream_readSize(is, &(self->size), true);
    
    /* zero-copy optimization */
    self->items = is->next;
    is->next += self->size;
}

typedef struct {
    Ice_Object __base;
} IDM_NeighborDiscovery_Listener;

typedef IDM_NeighborDiscovery_Listener* IDM_NeighborDiscovery_ListenerPtr;

typedef Ice_ObjectPrx IDM_NeighborDiscovery_ListenerPrx;
typedef Ice_ObjectPrx* IDM_NeighborDiscovery_ListenerPrxPtr;

/* forward declaration */
static void
IDM_NeighborDiscovery_Listener_methodHandler(IDM_NeighborDiscovery_ListenerPtr self,
                                             Ice_InputStreamPtr is,
                                             Ice_OutputStreamPtr os);

static void
IDM_NeighborDiscovery_Listener_init(IDM_NeighborDiscovery_ListenerPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)IDM_NeighborDiscovery_Listener_methodHandler);
}

void
IDM_NeighborDiscovery_ListenerI_adv(IDM_NeighborDiscovery_ListenerPtr self,
                                    Ice_String proxy) __attribute__((weak));

void
IDM_NeighborDiscovery_ListenerI_bye(IDM_NeighborDiscovery_ListenerPtr self,
                                    IDM_Address oid) __attribute__((weak));

static void
IDM_NeighborDiscovery_Listener_adv__dispatch(IDM_NeighborDiscovery_ListenerPtr self,
                                             Ice_InputStreamPtr is,
                                             Ice_OutputStreamPtr os) {
    Ice_String proxy;

    trace();
    
    Ice_InputStream_readIceString(is, &proxy);
    
    IDM_NeighborDiscovery_ListenerI_adv(self, proxy);
}

static void
IDM_NeighborDiscovery_Listener_bye__dispatch(IDM_NeighborDiscovery_ListenerPtr self,
                                             Ice_InputStreamPtr is,
                                             Ice_OutputStreamPtr os) {
    IDM_Address oid;

    trace();
    
    /* allocate space for sequence */
    Ice_InputStream_readSize(is, &oid.size, false);
    byte _data[sizeof(byte) * oid.size];
    oid.items = (byte*)_data;
    IDM_Address_readFromInputStream(&oid, is);
    
    IDM_NeighborDiscovery_ListenerI_bye(self, oid);
}

static void
IDM_NeighborDiscovery_Listener_methodHandler(IDM_NeighborDiscovery_ListenerPtr self,
                                             Ice_InputStreamPtr is,
                                             Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "adv") == 0) {
        IDM_NeighborDiscovery_Listener_adv__dispatch(self, is, os);
        return;
    }
    
    if (strcmp(operation, "bye") == 0) {
        IDM_NeighborDiscovery_Listener_bye__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static void
IDM_NeighborDiscovery_Listener_adv(IDM_NeighborDiscovery_ListenerPrxPtr self,
                                   Ice_String proxy) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "adv");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_writeIceString(os, proxy);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

static void
IDM_NeighborDiscovery_Listener_bye(IDM_NeighborDiscovery_ListenerPrxPtr self,
                                   IDM_Address oid) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "bye");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    IDM_Address_writeToOutputStream(&(oid), os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

typedef struct {
    Ice_Object __base;
} DUO_IDM_IBool_W;

typedef DUO_IDM_IBool_W* DUO_IDM_IBool_WPtr;

typedef Ice_ObjectPrx DUO_IDM_IBool_WPrx;
typedef Ice_ObjectPrx* DUO_IDM_IBool_WPrxPtr;

/* forward declaration */
static void
DUO_IDM_IBool_W_methodHandler(DUO_IDM_IBool_WPtr self,
                              Ice_InputStreamPtr is,
                              Ice_OutputStreamPtr os);

static void
DUO_IDM_IBool_W_init(DUO_IDM_IBool_WPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)DUO_IDM_IBool_W_methodHandler);
}

void
DUO_IDM_IBool_WI_set(DUO_IDM_IBool_WPtr self,
                     Ice_Bool v,
                     IDM_Address source) __attribute__((weak));

static void
DUO_IDM_IBool_W_set__dispatch(DUO_IDM_IBool_WPtr self,
                              Ice_InputStreamPtr is,
                              Ice_OutputStreamPtr os) {
    Ice_Bool v = 0;
    IDM_Address source;

    trace();
    
    Ice_InputStream_readBool(is, &v);
    
    /* allocate space for sequence */
    Ice_InputStream_readSize(is, &source.size, false);
    byte _data[sizeof(byte) * source.size];
    source.items = (byte*)_data;
    IDM_Address_readFromInputStream(&source, is);
    
    DUO_IDM_IBool_WI_set(self, v, source);
}

static void
DUO_IDM_IBool_W_methodHandler(DUO_IDM_IBool_WPtr self,
                              Ice_InputStreamPtr is,
                              Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "set") == 0) {
        DUO_IDM_IBool_W_set__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static void
DUO_IDM_IBool_W_set(DUO_IDM_IBool_WPrxPtr self,
                    Ice_Bool v,
                    IDM_Address source) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "set");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_writeBool(os, v);
    IDM_Address_writeToOutputStream(&(source), os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

typedef struct {
    Ice_Object __base;
} DUO_IDM_IInt_W;

typedef DUO_IDM_IInt_W* DUO_IDM_IInt_WPtr;

typedef Ice_ObjectPrx DUO_IDM_IInt_WPrx;
typedef Ice_ObjectPrx* DUO_IDM_IInt_WPrxPtr;

/* forward declaration */
static void
DUO_IDM_IInt_W_methodHandler(DUO_IDM_IInt_WPtr self,
                             Ice_InputStreamPtr is,
                             Ice_OutputStreamPtr os);

static void
DUO_IDM_IInt_W_init(DUO_IDM_IInt_WPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)DUO_IDM_IInt_W_methodHandler);
}

void
DUO_IDM_IInt_WI_set(DUO_IDM_IInt_WPtr self,
                    Ice_Int v,
                    IDM_Address source) __attribute__((weak));

static void
DUO_IDM_IInt_W_set__dispatch(DUO_IDM_IInt_WPtr self,
                             Ice_InputStreamPtr is,
                             Ice_OutputStreamPtr os) {
    Ice_Int v = 0;
    IDM_Address source;

    trace();
    
    Ice_InputStream_readInt(is, &v);
    
    /* allocate space for sequence */
    Ice_InputStream_readSize(is, &source.size, false);
    byte _data[sizeof(byte) * source.size];
    source.items = (byte*)_data;
    IDM_Address_readFromInputStream(&source, is);
    
    DUO_IDM_IInt_WI_set(self, v, source);
}

static void
DUO_IDM_IInt_W_methodHandler(DUO_IDM_IInt_WPtr self,
                             Ice_InputStreamPtr is,
                             Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "set") == 0) {
        DUO_IDM_IInt_W_set__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static void
DUO_IDM_IInt_W_set(DUO_IDM_IInt_WPrxPtr self,
                   Ice_Int v,
                   IDM_Address source) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "set");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_writeInt(os, v);
    IDM_Address_writeToOutputStream(&(source), os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

typedef struct {
    Ice_Object __base;
} DUO_IDM_Active_R;

typedef DUO_IDM_Active_R* DUO_IDM_Active_RPtr;

typedef Ice_ObjectPrx DUO_IDM_Active_RPrx;
typedef Ice_ObjectPrx* DUO_IDM_Active_RPrxPtr;

/* forward declaration */
static void
DUO_IDM_Active_R_methodHandler(DUO_IDM_Active_RPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os);

static void
DUO_IDM_Active_R_init(DUO_IDM_Active_RPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)DUO_IDM_Active_R_methodHandler);
}

IDM_Address
DUO_IDM_Active_RI_getObserver(DUO_IDM_Active_RPtr self) __attribute__((weak));

static void
DUO_IDM_Active_R_getObserver__dispatch(DUO_IDM_Active_RPtr self,
                                       Ice_InputStreamPtr is,
                                       Ice_OutputStreamPtr os) {
    IDM_Address _retval;

    trace();
    
    _retval = DUO_IDM_Active_RI_getObserver(self);
    IDM_Address_writeToOutputStream(&(_retval), os);
}

static void
DUO_IDM_Active_R_methodHandler(DUO_IDM_Active_RPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "getObserver") == 0) {
        DUO_IDM_Active_R_getObserver__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static IDM_Address
DUO_IDM_Active_R_getObserver(DUO_IDM_Active_RPrxPtr self) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "getObserver");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

typedef struct {
    Ice_Object __base;
} DUO_IDM_Active_W;

typedef DUO_IDM_Active_W* DUO_IDM_Active_WPtr;

typedef Ice_ObjectPrx DUO_IDM_Active_WPrx;
typedef Ice_ObjectPrx* DUO_IDM_Active_WPrxPtr;

/* forward declaration */
static void
DUO_IDM_Active_W_methodHandler(DUO_IDM_Active_WPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os);

static void
DUO_IDM_Active_W_init(DUO_IDM_Active_WPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)DUO_IDM_Active_W_methodHandler);
}

void
DUO_IDM_Active_WI_setObserver(DUO_IDM_Active_WPtr self,
                              IDM_Address observer) __attribute__((weak));

static void
DUO_IDM_Active_W_setObserver__dispatch(DUO_IDM_Active_WPtr self,
                                       Ice_InputStreamPtr is,
                                       Ice_OutputStreamPtr os) {
    IDM_Address observer;

    trace();
    
    /* allocate space for sequence */
    Ice_InputStream_readSize(is, &observer.size, false);
    byte _data[sizeof(byte) * observer.size];
    observer.items = (byte*)_data;
    IDM_Address_readFromInputStream(&observer, is);
    
    DUO_IDM_Active_WI_setObserver(self, observer);
}

static void
DUO_IDM_Active_W_methodHandler(DUO_IDM_Active_WPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "setObserver") == 0) {
        DUO_IDM_Active_W_setObserver__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static void
DUO_IDM_Active_W_setObserver(DUO_IDM_Active_WPrxPtr self,
                             IDM_Address observer) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "setObserver");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    IDM_Address_writeToOutputStream(&(observer), os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

typedef struct {
    Ice_Object __base;
} DUO_IDM_Listener;

typedef DUO_IDM_Listener* DUO_IDM_ListenerPtr;

typedef Ice_ObjectPrx DUO_IDM_ListenerPrx;
typedef Ice_ObjectPrx* DUO_IDM_ListenerPrxPtr;

/* forward declaration */
static void
DUO_IDM_Listener_methodHandler(DUO_IDM_ListenerPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os);

static void
DUO_IDM_Listener_init(DUO_IDM_ListenerPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)DUO_IDM_Listener_methodHandler);
}

void
DUO_IDM_ListenerI_adv(DUO_IDM_ListenerPtr self,
                      IDM_Address obj) __attribute__((weak));

void
DUO_IDM_ListenerI_bye(DUO_IDM_ListenerPtr self,
                      IDM_Address obj) __attribute__((weak));

static void
DUO_IDM_Listener_adv__dispatch(DUO_IDM_ListenerPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os) {
    IDM_Address obj;

    trace();
    
    /* allocate space for sequence */
    Ice_InputStream_readSize(is, &obj.size, false);
    byte _data[sizeof(byte) * obj.size];
    obj.items = (byte*)_data;
    IDM_Address_readFromInputStream(&obj, is);
    
    DUO_IDM_ListenerI_adv(self, obj);
}

static void
DUO_IDM_Listener_bye__dispatch(DUO_IDM_ListenerPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os) {
    IDM_Address obj;

    trace();
    
    /* allocate space for sequence */
    Ice_InputStream_readSize(is, &obj.size, false);
    byte _data[sizeof(byte) * obj.size];
    obj.items = (byte*)_data;
    IDM_Address_readFromInputStream(&obj, is);
    
    DUO_IDM_ListenerI_bye(self, obj);
}

static void
DUO_IDM_Listener_methodHandler(DUO_IDM_ListenerPtr self,
                               Ice_InputStreamPtr is,
                               Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "adv") == 0) {
        DUO_IDM_Listener_adv__dispatch(self, is, os);
        return;
    }
    
    if (strcmp(operation, "bye") == 0) {
        DUO_IDM_Listener_bye__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static void
DUO_IDM_Listener_adv(DUO_IDM_ListenerPrxPtr self,
                     IDM_Address obj) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "adv");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    IDM_Address_writeToOutputStream(&(obj), os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

static void
DUO_IDM_Listener_bye(DUO_IDM_ListenerPrxPtr self,
                     IDM_Address obj) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "bye");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    IDM_Address_writeToOutputStream(&(obj), os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

typedef struct {
    Ice_Object __base;
} IoT_IBool;

typedef IoT_IBool* IoT_IBoolPtr;

typedef Ice_ObjectPrx IoT_IBoolPrx;
typedef Ice_ObjectPrx* IoT_IBoolPrxPtr;

/* forward declaration */
static void
IoT_IBool_methodHandler(IoT_IBoolPtr self,
                        Ice_InputStreamPtr is,
                        Ice_OutputStreamPtr os);

static void
IoT_IBool_init(IoT_IBoolPtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)IoT_IBool_methodHandler);
}

static void
IoT_IBool_methodHandler(IoT_IBoolPtr self,
                        Ice_InputStreamPtr is,
                        Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "setObserver") == 0) {
        DUO_IDM_Active_W_setObserver__dispatch((DUO_IDM_Active_WPtr)self, is, os);
        return;
    }
    
    if (strcmp(operation, "set") == 0) {
        DUO_IDM_IBool_W_set__dispatch((DUO_IDM_IBool_WPtr)self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

typedef struct {
    Ice_Object __base;
} IoT_Node;

typedef IoT_Node* IoT_NodePtr;

typedef Ice_ObjectPrx IoT_NodePrx;
typedef Ice_ObjectPrx* IoT_NodePrxPtr;

/* forward declaration */
static void
IoT_Node_methodHandler(IoT_NodePtr self,
                       Ice_InputStreamPtr is,
                       Ice_OutputStreamPtr os);

static void
IoT_Node_init(IoT_NodePtr self) {

    trace();
    
    Ice_Object_init((Ice_ObjectPtr)self,
            (Ice_MethodHandlerPtr)IoT_Node_methodHandler);
}

void
IoT_NodeI_setupWiFi(IoT_NodePtr self,
                    Ice_String ssid,
                    Ice_String key) __attribute__((weak));

void
IoT_NodeI_setIDMRouter(IoT_NodePtr self,
                       Ice_String proxy) __attribute__((weak));

void
IoT_NodeI_restart(IoT_NodePtr self) __attribute__((weak));

void
IoT_NodeI_factoryReset(IoT_NodePtr self) __attribute__((weak));

static void
IoT_Node_setupWiFi__dispatch(IoT_NodePtr self,
                             Ice_InputStreamPtr is,
                             Ice_OutputStreamPtr os) {
    Ice_String ssid;
    Ice_String key;

    trace();
    
    Ice_InputStream_readIceString(is, &ssid);
    
    Ice_InputStream_readIceString(is, &key);
    
    IoT_NodeI_setupWiFi(self, ssid, key);
}

static void
IoT_Node_setIDMRouter__dispatch(IoT_NodePtr self,
                                Ice_InputStreamPtr is,
                                Ice_OutputStreamPtr os) {
    Ice_String proxy;

    trace();
    
    Ice_InputStream_readIceString(is, &proxy);
    
    IoT_NodeI_setIDMRouter(self, proxy);
}

static void
IoT_Node_restart__dispatch(IoT_NodePtr self,
                           Ice_InputStreamPtr is,
                           Ice_OutputStreamPtr os) {

    trace();
    
    IoT_NodeI_restart(self);
}

static void
IoT_Node_factoryReset__dispatch(IoT_NodePtr self,
                                Ice_InputStreamPtr is,
                                Ice_OutputStreamPtr os) {

    trace();
    
    IoT_NodeI_factoryReset(self);
}

static void
IoT_Node_methodHandler(IoT_NodePtr self,
                       Ice_InputStreamPtr is,
                       Ice_OutputStreamPtr os) {
    Ice_Int operationSize = 0;
    char operation[MAX_OPERATION_NAME_SIZE];
    byte mode = 0;
    byte contextSize = 0;
    Ice_Int encapSize = 0;
    byte major = 0;
    byte minor = 0;

    trace();
    
    Ptr_check(self);
    Ptr_check(is);
    Ptr_check(os);
    
    Ice_InputStream_readSize(is, &operationSize, false);
    assert(operationSize <= MAX_OPERATION_NAME_SIZE);
    
    Ice_InputStream_readString(is, operation);
    
    /* FIXME: operationMode ignored */
    Ice_InputStream_readByte(is, &mode);
    
    /* FIXME: Context not supported */
    Ice_InputStream_readByte(is, &contextSize);
    assert(contextSize == 0);
    
    Ice_InputStream_readInt(is, &encapSize);
    Ice_InputStream_readByte(is, &major);
    Ice_InputStream_readByte(is, &minor);
    assert(major == 1 && minor == 0);
    assert(encapSize - 6 == (is->size - (is->next - is->data)));
    
    if (strcmp(operation, "setupWiFi") == 0) {
        IoT_Node_setupWiFi__dispatch(self, is, os);
        return;
    }
    
    if (strcmp(operation, "setIDMRouter") == 0) {
        IoT_Node_setIDMRouter__dispatch(self, is, os);
        return;
    }
    
    if (strcmp(operation, "restart") == 0) {
        IoT_Node_restart__dispatch(self, is, os);
        return;
    }
    
    if (strcmp(operation, "factoryReset") == 0) {
        IoT_Node_factoryReset__dispatch(self, is, os);
        return;
    }
    
    c_throw("Ice_OperationNotExistException", __FILE__, __LINE__);
}

static void
IoT_Node_setupWiFi(IoT_NodePrxPtr self,
                   Ice_String ssid,
                   Ice_String key) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "setupWiFi");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_writeIceString(os, ssid);
    Ice_OutputStream_writeIceString(os, key);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

static void
IoT_Node_setIDMRouter(IoT_NodePrxPtr self,
                      Ice_String proxy) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "setIDMRouter");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_writeIceString(os, proxy);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

static void
IoT_Node_restart(IoT_NodePrxPtr self) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "restart");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

static void
IoT_Node_factoryReset(IoT_NodePrxPtr self) {
    Ice_OutputStreamPtr os = &(self->stream);

    trace();
    
    Ptr_check(self);
    
    Ice_ObjectPrx_connect(self);
    Ice_OutputStream_init(os);
    
    /* request header */
    Ice_OutputStream_writeBlob(os, Ice_header, sizeof(Ice_header));
    
    /* request body */
    Ice_OutputStream_writeInt(os, 0);
    Ice_OutputStream_writeIdentity(os, self->identity);
    Ice_OutputStream_writeString(os, "");
    Ice_OutputStream_writeString(os, "factoryReset");
    Ice_OutputStream_writeByte(os, Ice_Normal);
    Ice_OutputStream_writeByte(os, 0);
    
    /* encapsulated params */
    Ice_OutputStream_startWriteEncaps(os);
    Ice_OutputStream_endWriteEncaps(os);
    Ice_OutputStream_setMessageSize(os);
    
    Ice_ObjectPrx_send(self);
    
    /* FIXME: wait response and return correct value */
    
    Ice_ObjectPrx_closeConnection(self);
}

