import React from 'react';
import {
  Container,
  FormField,
  Header,
  Input,
  RadioGroup,
  Select,
} from "@cloudscape-design/components";

type SourceClusterFormProps = {
  formValues: any;
  partialStateUpdate: (update: Partial<any>) => void;
  regionList: string[];
};

const SourceClusterForm: React.FC<SourceClusterFormProps> = ({
  formValues,
  partialStateUpdate,
  regionList,
}) => {
  return (
    <Container header={<Header variant="h2">Source Cluster</Header>}>
      <FormField
        label="Endpoint"
        description="The endpoint, including port if necessary, of the existing cluster."
      >
        <Input
          value={formValues?.sourceClusterEndpoint ?? ""}
          onChange={({ detail }) =>
            partialStateUpdate({
              sourceClusterEndpoint: detail.value,
            })
          }
        />
      </FormField>
      <FormField
        label="Auth"
        description="The authorization type of the existing cluster."
      >
        <RadioGroup
          onChange={({ detail }) => {
            partialStateUpdate({
              sourceClusterAuthType: detail.value,
              sourceClusterAuthUsername: undefined,
              sourceClusterAuthPasswordFromArn: undefined,
              sourceClusterAuthServiceSigningName: undefined,
              sourceClusterAuthRegion: undefined,
            });
          }}
          value={formValues?.sourceClusterAuthType}
          items={[
            { value: "none", label: "No authorization" },
            {
              value: "basic",
              label: "Basic authorization with a username and password",
            },
            { value: "sigv4", label: "SigV4 signing" },
          ]}
        />
      </FormField>
      {formValues.sourceClusterAuthType === "basic" && (
        <Container>
          <FormField
            label="Username"
            description="Username of the master user on the existing cluster"
          >
            <Input
              value={formValues?.sourceClusterAuthUsername ?? ""}
              onChange={({ detail }) =>
                partialStateUpdate({
                  sourceClusterAuthUsername: detail.value,
                })
              }
            />
          </FormField>
          <FormField
            label="Password from Secret ARN"
            description="ARN of a Secrets Manager secret containing the password of the master user on the existing cluster"
          >
            <Input
              value={formValues?.sourceClusterAuthPasswordFromArn ?? ""}
              onChange={({ detail }) =>
                partialStateUpdate({
                  sourceClusterAuthPasswordFromArn: detail.value,
                })
              }
            />
          </FormField>
        </Container>
      )}
      {formValues.sourceClusterAuthType === "sigv4" && (
        <Container>
          <FormField
            label="Service Signing Name"
            description="The name of the service to use for SigV4 signing"
          >
            <RadioGroup
              onChange={({ detail }) =>
                partialStateUpdate({
                  sourceClusterAuthServiceSigningName: detail.value,
                })
              }
              value={formValues?.sourceClusterAuthServiceSigningName ?? null}
              items={[
                {
                  value: "es",
                  label: "Amazon Elasticsearch and OpenSearch Managed Service",
                },
                {
                  value: "aoss",
                  label: "Amazon OpenSearch Serverless Service",
                },
              ]}
            />
          </FormField>
          <FormField
            label="Region"
            description="The region of the existing cluster"
          >
            <Select
              selectedOption={{
                value: formValues.sourceClusterAuthRegion,
              }}
              onChange={({ detail }) =>
                partialStateUpdate({
                  sourceClusterAuthRegion: detail.selectedOption.value,
                })
              }
              options={regionList.map((r) => ({
                value: r,
                label: r,
              }))}
            />
          </FormField>
        </Container>
      )}
    </Container>
  );
};

export default SourceClusterForm;