import React, { createContext, useMemo, useContext } from 'react';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import TreeDataSet from './TreeDataSet';
import { useDeploymentStore } from '../../../stores';

const Store = createContext();

export function useSidebarStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const { AppState: { currentMenuType: { id } }, children } = props;
    const { deploymentStore } = useDeploymentStore();
    const treeDs = useMemo(() => new DataSet(TreeDataSet(id, deploymentStore)), [deploymentStore, id]);

    const value = {
      ...props,
      treeDs,
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  },
));
