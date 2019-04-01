%define __jar_repack {%nil}
# Compatibility with ncar
%define _source_payload w0.gzdio
%define _binary_payload w0.gzdio
%define _binary_filedigest_algorithm 1

# For use below
%define _prefix %{_usr}/local/chronopolis/replication
%define jar replicationd.jar
%define yaml application.yml
%define prep replicationd-prepare
%define service /usr/lib/systemd/system/replicationd.service
%define build_date %(date +"%Y%m%d")

Name: replicationd
Version: %{ver}
Release: %{build_date}.el7
Source: replicationd.service
Source1: replication-shell.jar
Source2: replication-application.yml
Source3: replicationd-prepare.sh
Summary: Chronopolis Replication Service
License: BSD-3
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description

The Replication Services monitors for packages being ingested into Chronopolis
and does replication and registration on them.

%preun

systemctl disable replicationd

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{service}"
%__install -D -m0644 "%{SOURCE1}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"
%__install -D -m0755 "%{SOURCE3}" "%{buildroot}%{_prefix}/%{prep}"

%files

%defattr(-,root,root)
%dir %{_prefix}

%{service}
%{_prefix}/%{jar}
%{_prefix}/%{prep}
%config(noreplace) %{_prefix}/%{yaml}

%changelog

* Tue Mar 5 2019 Mike Ritter <shake@umiacs.umd.edu> 3.1.0-20190305
- Set license to BSD 3 clause

* Fri Dec 1 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.5-20171201
- fix default modebits for prepare script

* Wed Nov 8 2017 Mike Ritter <shake@umiacs.umd.edu> 2.0.3-20171108
- add replicationd-prepare script
- remove install commands for logging directory

* Tue Oct 3 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171003
- cleanup spec to include missing sections
